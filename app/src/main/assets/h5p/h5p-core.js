/**
 * Lumi Mobile - Offline H5P Interactive Core Runner
 * Compatible with H5P content types and Lumi desktop specifications.
 */
(function() {
  window.H5P = window.H5P || {};
  window.H5P.instances = [];

  let currentManifest = null;
  let currentContent = null;
  let currentScore = 0;
  let maxScore = 0;

  function stripHtml(html) {
    if (!html) return '';
    const tmp = document.createElement("DIV");
    tmp.innerHTML = html;
    return tmp.textContent || tmp.innerText || "";
  }

  function notifyBridge(event, data) {
    try {
      if (window.LumiBridge) {
        if (event === 'score' && window.LumiBridge.onScore) {
          window.LumiBridge.onScore(Number(data.score) || 0, Number(data.maxScore) || 0);
        } else if (event === 'progress' && window.LumiBridge.onProgress) {
          window.LumiBridge.onProgress(Number(data.current) || 0, Number(data.total) || 0);
        } else if (event === 'loaded' && window.LumiBridge.onActivityLoaded) {
          window.LumiBridge.onActivityLoaded(data.title || '', data.library || '');
        }
      }
    } catch (err) {
      console.warn("LumiBridge notification failed safely:", err);
    }
  }

  function updateScoreDisplay(score, max) {
    currentScore = score;
    maxScore = max;
    const scoreBar = document.getElementById('score-bar');
    const scoreText = document.getElementById('score-text');
    if (scoreBar && scoreText && max > 0) {
      scoreBar.style.display = 'inline-flex';
      scoreText.textContent = `${score} / ${max}`;
    }
    notifyBridge('score', { score: score, maxScore: max });
  }

  // MultiChoice Renderer
  function renderMultiChoice(container, content) {
    const questionHtml = content.question || 'Choose the correct answer:';
    const answers = content.answers || [];
    const isSingle = (content.behaviour && content.behaviour.singlePoint) || 
                     (content.answers && content.answers.filter(a => a.correct).length <= 1);
    const inputType = isSingle ? 'radio' : 'checkbox';
    
    maxScore = isSingle ? 1 : answers.filter(a => a.correct).length;
    let checkedIndices = new Set();
    let isEvaluated = false;

    // Randomize option positions (default behavior in H5P)
    let displayedAnswers = answers.map((ans, origIdx) => ({ ...ans, origIdx }));
    const shouldRandomize = content.behaviour ? content.behaviour.randomAnswers !== false : true;
    if (shouldRandomize && displayedAnswers.length > 1) {
      for (let i = displayedAnswers.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [displayedAnswers[i], displayedAnswers[j]] = [displayedAnswers[j], displayedAnswers[i]];
      }
    }

    let html = `
      <div class="h5p-question-prompt">${questionHtml}</div>
      <div class="h5p-options-list" id="mc-options">
    `;

    displayedAnswers.forEach((ans) => {
      const idx = ans.origIdx;
      const text = ans.text || '';
      html += `
        <label class="h5p-option" data-index="${idx}" id="mc-opt-${idx}">
          <input type="${inputType}" name="h5p-choice" value="${idx}">
          <span class="h5p-option-text">${text}</span>
        </label>
      `;
    });

    html += `</div>
      <div class="h5p-actions">
        <button class="h5p-btn h5p-btn-primary" id="mc-check-btn">Check Answer</button>
        <button class="h5p-btn h5p-btn-secondary" id="mc-retry-btn" style="display: none;">Retry</button>
        <button class="h5p-btn h5p-btn-ghost" id="mc-solution-btn" style="display: none;">Show Solution</button>
      </div>
    `;

    container.innerHTML = html;

    const optionEls = container.querySelectorAll('.h5p-option');
    optionEls.forEach(el => {
      el.addEventListener('click', function(e) {
        if (isEvaluated) return;
        const idx = parseInt(el.getAttribute('data-index'));
        const input = el.querySelector('input');
        if (isSingle) {
          checkedIndices.clear();
          checkedIndices.add(idx);
          optionEls.forEach(o => o.classList.remove('selected'));
          el.classList.add('selected');
        } else {
          if (checkedIndices.has(idx)) {
            checkedIndices.delete(idx);
            el.classList.remove('selected');
          } else {
            checkedIndices.add(idx);
            el.classList.add('selected');
          }
        }
      });
    });

    const checkBtn = document.getElementById('mc-check-btn');
    const retryBtn = document.getElementById('mc-retry-btn');
    const solutionBtn = document.getElementById('mc-solution-btn');

    checkBtn.addEventListener('click', () => {
      if (checkedIndices.size === 0) {
        alert("Please select at least one option.");
        return;
      }
      isEvaluated = true;
      let score = 0;

      answers.forEach((ans, idx) => {
        const optEl = document.getElementById(`mc-opt-${idx}`);
        const isSelected = checkedIndices.has(idx);
        const isCorrect = ans.correct === true;

        if (isSelected && isCorrect) {
          optEl.classList.add('correct');
          score++;
        } else if (isSelected && !isCorrect) {
          optEl.classList.add('incorrect');
        } else if (!isSelected && isCorrect) {
          // missed correct option
        }

        // Show feedback if available
        if (ans.tipsAndFeedback && ans.tipsAndFeedback.chosenFeedback && isSelected) {
          const feedback = document.createElement('div');
          feedback.className = 'h5p-feedback-msg';
          feedback.innerHTML = ans.tipsAndFeedback.chosenFeedback;
          optEl.appendChild(feedback);
        }
      });

      if (isSingle) {
        score = (score > 0) ? 1 : 0;
      }

      updateScoreDisplay(score, maxScore);
      checkBtn.style.display = 'none';
      retryBtn.style.display = 'inline-flex';
      solutionBtn.style.display = 'inline-flex';
    });

    retryBtn.addEventListener('click', () => {
      renderMultiChoice(container, content);
      updateScoreDisplay(0, maxScore);
    });

    solutionBtn.addEventListener('click', () => {
      answers.forEach((ans, idx) => {
        const optEl = document.getElementById(`mc-opt-${idx}`);
        if (ans.correct) {
          optEl.classList.add('correct');
        }
      });
    });

    updateScoreDisplay(0, maxScore);
  }

  // Dialogcards / Flashcards Renderer
  function renderDialogCards(container, content) {
    const dialogs = content.dialogs || [];
    if (dialogs.length === 0) {
      container.innerHTML = '<p>No cards found in package.</p>';
      return;
    }

    let currentIndex = 0;
    maxScore = dialogs.length;
    let knownCount = 0;
    let ratedCards = new Set();

    function updateCardView() {
      const cardData = dialogs[currentIndex];
      const isFlipped = false;
      notifyBridge('progress', { current: currentIndex + 1, total: dialogs.length });

      container.innerHTML = `
        <div class="h5p-card-nav">
          <span style="font-size: 0.9rem; font-weight: 600; color: var(--text-muted);">
            Card ${currentIndex + 1} of ${dialogs.length}
          </span>
          <span style="font-size: 0.85rem; color: var(--primary);">
            Tap card to flip
          </span>
        </div>

        <div class="h5p-card-viewport">
          <div class="h5p-card" id="active-flashcard">
            <div class="h5p-card-face h5p-card-front">
              <div class="h5p-card-prompt">${cardData.text || 'Card Front'}</div>
              ${cardData.tips ? `<div class="h5p-card-hint">Hint: ${cardData.tips}</div>` : ''}
              <div class="h5p-card-flip-prompt">Tap to reveal answer</div>
            </div>
            <div class="h5p-card-face h5p-card-back">
              <div class="h5p-card-answer">${cardData.answer || 'Card Back'}</div>
              <div class="h5p-card-flip-prompt">Tap to flip back</div>
            </div>
          </div>
        </div>

        <div class="h5p-actions" style="justify-content: space-between;">
          <button class="h5p-btn h5p-btn-secondary" id="card-prev-btn" ${currentIndex === 0 ? 'disabled style="opacity: 0.4;"' : ''}>
            Previous
          </button>
          <div style="display: flex; gap: 8px;">
            <button class="h5p-btn h5p-btn-ghost" id="card-known-btn" style="color: var(--success); font-weight: 700;">
              Got it!
            </button>
            <button class="h5p-btn h5p-btn-primary" id="card-next-btn">
              ${currentIndex === dialogs.length - 1 ? 'Finish Deck' : 'Next'}
            </button>
          </div>
        </div>
      `;

      const cardEl = document.getElementById('active-flashcard');
      cardEl.addEventListener('click', () => {
        cardEl.classList.toggle('is-flipped');
      });

      document.getElementById('card-prev-btn').addEventListener('click', () => {
        if (currentIndex > 0) {
          currentIndex--;
          updateCardView();
        }
      });

      document.getElementById('card-known-btn').addEventListener('click', () => {
        if (!ratedCards.has(currentIndex)) {
          ratedCards.add(currentIndex);
          knownCount++;
          updateScoreDisplay(knownCount, maxScore);
        }
        if (currentIndex < dialogs.length - 1) {
          currentIndex++;
          updateCardView();
        } else {
          showDeckComplete();
        }
      });

      document.getElementById('card-next-btn').addEventListener('click', () => {
        if (currentIndex < dialogs.length - 1) {
          currentIndex++;
          updateCardView();
        } else {
          showDeckComplete();
        }
      });
    }

    function showDeckComplete() {
      container.innerHTML = `
        <div style="text-align: center; padding: 24px;">
          <div style="font-size: 2.5rem; margin-bottom: 12px;">🌟</div>
          <h2>Deck Completed!</h2>
          <p style="color: var(--text-muted); margin: 8px 0 20px;">
            You mastered ${knownCount} out of ${dialogs.length} cards in this deck.
          </p>
          <div class="h5p-actions" style="justify-content: center;">
            <button class="h5p-btn h5p-btn-primary" id="deck-restart-btn">Review Again</button>
          </div>
        </div>
      `;
      document.getElementById('deck-restart-btn').addEventListener('click', () => {
        currentIndex = 0;
        knownCount = 0;
        ratedCards.clear();
        updateScoreDisplay(0, maxScore);
        updateCardView();
      });
    }

    updateCardView();
    updateScoreDisplay(0, maxScore);
  }

  // Blanks (Fill in the blanks) Renderer
  function renderBlanks(container, content) {
    const promptText = content.text || 'Fill in the blanks:';
    const rawQuestions = content.questions || [];
    const caseSensitive = content.behaviour && content.behaviour.caseSensitive === true;
    
    // Parse blanks marked like *answer* or *answer/alt1/alt2*
    let parsedItems = [];
    let blankCounter = 0;

    rawQuestions.forEach((qStr) => {
      const regex = /\*([^*]+)\*/g;
      let lastIdx = 0;
      let segments = [];
      let match;

      while ((match = regex.exec(qStr)) !== null) {
        if (match.index > lastIdx) {
          segments.push({ type: 'text', value: qStr.substring(lastIdx, match.index) });
        }
        const answers = match[1].split('/').map(s => s.trim());
        segments.push({ type: 'blank', index: blankCounter++, accepted: answers });
        lastIdx = regex.lastIndex;
      }
      if (lastIdx < qStr.length) {
        segments.push({ type: 'text', value: qStr.substring(lastIdx) });
      }
      parsedItems.push(segments);
    });

    maxScore = blankCounter;

    let html = `
      <div class="h5p-question-prompt">${promptText}</div>
      <div class="h5p-blanks-text">
    `;

    parsedItems.forEach(segments => {
      html += '<p style="margin-bottom: 14px;">';
      segments.forEach(seg => {
        if (seg.type === 'text') {
          html += seg.value;
        } else {
          html += `<input type="text" class="h5p-blank-input" id="blank-${seg.index}" data-blank-index="${seg.index}" autocomplete="off" autocorrect="off" autocapitalize="off">`;
          html += `<span class="h5p-blank-solution" id="blank-sol-${seg.index}" style="display: none;"></span>`;
        }
      });
      html += '</p>';
    });

    html += `</div>
      <div class="h5p-actions">
        <button class="h5p-btn h5p-btn-primary" id="blanks-check-btn">Check</button>
        <button class="h5p-btn h5p-btn-secondary" id="blanks-retry-btn" style="display: none;">Retry</button>
        <button class="h5p-btn h5p-btn-ghost" id="blanks-solution-btn" style="display: none;">Show Solution</button>
      </div>
    `;

    container.innerHTML = html;

    const checkBtn = document.getElementById('blanks-check-btn');
    const retryBtn = document.getElementById('blanks-retry-btn');
    const solutionBtn = document.getElementById('blanks-solution-btn');

    checkBtn.addEventListener('click', () => {
      let score = 0;
      parsedItems.forEach(segments => {
        segments.forEach(seg => {
          if (seg.type === 'blank') {
            const inputEl = document.getElementById(`blank-${seg.index}`);
            const solEl = document.getElementById(`blank-sol-${seg.index}`);
            const userVal = inputEl.value.trim();
            
            const isMatch = seg.accepted.some(acc => {
              return caseSensitive ? acc === userVal : acc.toLowerCase() === userVal.toLowerCase();
            });

            if (isMatch && userVal.length > 0) {
              inputEl.classList.remove('incorrect');
              inputEl.classList.add('correct');
              score++;
            } else {
              inputEl.classList.remove('correct');
              inputEl.classList.add('incorrect');
            }
          }
        });
      });

      updateScoreDisplay(score, maxScore);
      checkBtn.style.display = 'none';
      retryBtn.style.display = 'inline-flex';
      solutionBtn.style.display = 'inline-flex';
    });

    retryBtn.addEventListener('click', () => {
      renderBlanks(container, content);
      updateScoreDisplay(0, maxScore);
    });

    solutionBtn.addEventListener('click', () => {
      parsedItems.forEach(segments => {
        segments.forEach(seg => {
          if (seg.type === 'blank') {
            const inputEl = document.getElementById(`blank-${seg.index}`);
            const solEl = document.getElementById(`blank-sol-${seg.index}`);
            inputEl.value = seg.accepted[0];
            inputEl.classList.remove('incorrect');
            inputEl.classList.add('correct');
          }
        });
      });
    });

    updateScoreDisplay(0, maxScore);
  }

  // True / False Renderer
  function renderTrueFalse(container, content) {
    const questionHtml = content.question || 'Is the following statement true or false?';
    const isCorrectTrue = content.correct === true || content.correct === 'true';
    maxScore = 1;
    let selectedAnswer = null;

    container.innerHTML = `
      <div class="h5p-question-prompt">${questionHtml}</div>
      <div class="h5p-options-list">
        <label class="h5p-option" id="tf-opt-true">
          <input type="radio" name="h5p-tf" value="true">
          <span class="h5p-option-text" style="font-weight: 700;">True</span>
        </label>
        <label class="h5p-option" id="tf-opt-false">
          <input type="radio" name="h5p-tf" value="false">
          <span class="h5p-option-text" style="font-weight: 700;">False</span>
        </label>
      </div>
      <div class="h5p-actions">
        <button class="h5p-btn h5p-btn-primary" id="tf-check-btn">Check</button>
        <button class="h5p-btn h5p-btn-secondary" id="tf-retry-btn" style="display: none;">Retry</button>
      </div>
    `;

    const optTrue = document.getElementById('tf-opt-true');
    const optFalse = document.getElementById('tf-opt-false');
    const checkBtn = document.getElementById('tf-check-btn');
    const retryBtn = document.getElementById('tf-retry-btn');

    optTrue.addEventListener('click', () => {
      selectedAnswer = true;
      optTrue.classList.add('selected');
      optFalse.classList.remove('selected');
    });

    optFalse.addEventListener('click', () => {
      selectedAnswer = false;
      optFalse.classList.add('selected');
      optTrue.classList.remove('selected');
    });

    checkBtn.addEventListener('click', () => {
      if (selectedAnswer === null) {
        alert("Please select True or False.");
        return;
      }
      const score = (selectedAnswer === isCorrectTrue) ? 1 : 0;
      if (isCorrectTrue) {
        optTrue.classList.add('correct');
        if (selectedAnswer === false) optFalse.classList.add('incorrect');
      } else {
        optFalse.classList.add('correct');
        if (selectedAnswer === true) optTrue.classList.add('incorrect');
      }

      const feedbackText = score === 1 
        ? (content.feedbackOnCorrect || 'Correct! Well done.')
        : (content.feedbackOnWrong || 'Incorrect.');
      
      const fbEl = document.createElement('div');
      fbEl.className = 'h5p-feedback-msg';
      fbEl.style.marginTop = '12px';
      fbEl.style.fontWeight = '600';
      fbEl.style.color = score === 1 ? 'var(--success)' : 'var(--error)';
      fbEl.textContent = feedbackText;
      container.querySelector('.h5p-options-list').appendChild(fbEl);

      updateScoreDisplay(score, 1);
      checkBtn.style.display = 'none';
      retryBtn.style.display = 'inline-flex';
    });

    retryBtn.addEventListener('click', () => {
      renderTrueFalse(container, content);
      updateScoreDisplay(0, 1);
    });

    updateScoreDisplay(0, 1);
  }

  // Single Choice Set Renderer
  function renderSingleChoiceSet(container, content) {
    const choices = content.choices || [];
    if (choices.length === 0) {
      container.innerHTML = '<p>No questions in single choice set.</p>';
      return;
    }

    let currentIndex = 0;
    let totalScore = 0;
    maxScore = choices.length;

    function renderQuestion() {
      const q = choices[currentIndex];
      const prompt = q.question || 'Select the correct answer:';
      const answers = q.answers || [];
      const correctAnswer = answers[0] || '';
      
      // Shuffle choices for presentation
      const shuffled = answers.map((ans, i) => ({ text: ans, isCorrect: i === 0 }))
        .sort(() => Math.random() - 0.5);

      notifyBridge('progress', { current: currentIndex + 1, total: choices.length });

      container.innerHTML = `
        <div style="display: flex; justify-content: space-between; margin-bottom: 12px; font-size: 0.85rem; color: var(--text-muted); font-weight: 600;">
          <span>Question ${currentIndex + 1} of ${choices.length}</span>
          <span>Score: ${totalScore}</span>
        </div>
        <div class="h5p-question-prompt">${prompt}</div>
        <div class="h5p-options-list">
          ${shuffled.map((opt, i) => `
            <div class="h5p-option scs-opt" data-correct="${opt.isCorrect}" id="scs-opt-${i}">
              <span class="h5p-option-text">${opt.text}</span>
            </div>
          `).join('')}
        </div>
      `;

      const optEls = container.querySelectorAll('.scs-opt');
      let answered = false;

      optEls.forEach(el => {
        el.addEventListener('click', () => {
          if (answered) return;
          answered = true;
          const isCorrect = el.getAttribute('data-correct') === 'true';
          if (isCorrect) {
            el.classList.add('correct');
            totalScore++;
          } else {
            el.classList.add('incorrect');
            optEls.forEach(o => {
              if (o.getAttribute('data-correct') === 'true') o.classList.add('correct');
            });
          }
          updateScoreDisplay(totalScore, maxScore);

          setTimeout(() => {
            if (currentIndex < choices.length - 1) {
              currentIndex++;
              renderQuestion();
            } else {
              showSummary();
            }
          }, 800);
        });
      });
    }

    function showSummary() {
      container.innerHTML = `
        <div style="text-align: center; padding: 20px;">
          <div style="font-size: 2.5rem; margin-bottom: 10px;">🏆</div>
          <h2>Set Completed!</h2>
          <p style="margin: 8px 0 20px; font-size: 1.1rem; color: var(--text-muted);">
            Final Result: <strong>${totalScore} / ${maxScore}</strong>
          </p>
          <div class="h5p-actions" style="justify-content: center;">
            <button class="h5p-btn h5p-btn-primary" id="scs-restart-btn">Play Set Again</button>
          </div>
        </div>
      `;
      document.getElementById('scs-restart-btn').addEventListener('click', () => {
        currentIndex = 0;
        totalScore = 0;
        updateScoreDisplay(0, maxScore);
        renderQuestion();
      });
    }

    renderQuestion();
    updateScoreDisplay(0, maxScore);
  }

  // Drag the Words Renderer
  function renderDragText(container, content) {
    const prompt = content.taskDescription || 'Drag or tap the words to fill in the blanks:';
    const text = content.textField || '';
    const regex = /\*([^*]+)\*/g;
    let match;
    let blanks = [];
    let wordBank = [];
    let counter = 0;

    while ((match = regex.exec(text)) !== null) {
      const word = match[1].trim();
      blanks.push({ index: counter++, word: word });
      wordBank.push(word);
    }

    maxScore = blanks.length;
    let userAssignments = {};
    let selectedWord = null;

    // Shuffle word bank
    const shuffledWords = [...wordBank].sort(() => Math.random() - 0.5);

    function updateView() {
      let parsedPassage = text;
      blanks.forEach(b => {
        const assigned = userAssignments[b.index] || '';
        const blankSlot = `<span class="h5p-drop-target" data-index="${b.index}" style="display: inline-block; min-width: 80px; padding: 4px 10px; margin: 0 4px; border: 2px dashed var(--primary); border-radius: 6px; background: var(--bg-color); text-align: center; font-weight: 600; cursor: pointer;">${assigned || '______'}</span>`;
        parsedPassage = parsedPassage.replace(`*${b.word}*`, blankSlot);
      });

      container.innerHTML = `
        <div class="h5p-question-prompt">${prompt}</div>
        <div style="line-height: 2.2; font-size: 1.05rem; margin-bottom: 20px;" id="drag-passage">
          ${parsedPassage}
        </div>
        <div style="font-size: 0.85rem; font-weight: 700; color: var(--text-muted); margin-bottom: 8px;">WORD BANK (TAP WORD, THEN TAP BLANK):</div>
        <div style="display: flex; flex-wrap: wrap; gap: 8px; margin-bottom: 20px;" id="word-bank">
          ${shuffledWords.map((w, idx) => {
            const isAssigned = Object.values(userAssignments).includes(w);
            return `
              <button class="h5p-btn h5p-btn-secondary word-chip" data-word="${w}" style="padding: 6px 14px; font-size: 0.9rem; ${isAssigned ? 'opacity: 0.35;' : ''} ${selectedWord === w ? 'background: var(--primary); color: white;' : ''}">
                ${w}
              </button>
            `;
          }).join('')}
        </div>
        <div class="h5p-actions">
          <button class="h5p-btn h5p-btn-primary" id="drag-check-btn">Check</button>
          <button class="h5p-btn h5p-btn-secondary" id="drag-retry-btn" style="display: none;">Retry</button>
        </div>
      `;

      container.querySelectorAll('.word-chip').forEach(btn => {
        btn.addEventListener('click', () => {
          selectedWord = btn.getAttribute('data-word');
          updateView();
        });
      });

      container.querySelectorAll('.h5p-drop-target').forEach(slot => {
        slot.addEventListener('click', () => {
          const idx = parseInt(slot.getAttribute('data-index'));
          if (selectedWord) {
            userAssignments[idx] = selectedWord;
            selectedWord = null;
            updateView();
          } else if (userAssignments[idx]) {
            delete userAssignments[idx];
            updateView();
          }
        });
      });

      const checkBtn = document.getElementById('drag-check-btn');
      const retryBtn = document.getElementById('drag-retry-btn');

      checkBtn.addEventListener('click', () => {
        let score = 0;
        blanks.forEach(b => {
          const slot = container.querySelector(`.h5p-drop-target[data-index="${b.index}"]`);
          const userVal = userAssignments[b.index];
          if (userVal && userVal.toLowerCase() === b.word.toLowerCase()) {
            score++;
            if (slot) {
              slot.style.borderColor = 'var(--success)';
              slot.style.background = 'var(--success-bg)';
              slot.style.color = 'var(--success)';
            }
          } else {
            if (slot) {
              slot.style.borderColor = 'var(--error)';
              slot.style.background = 'var(--error-bg)';
              slot.style.color = 'var(--error)';
            }
          }
        });

        updateScoreDisplay(score, maxScore);
        checkBtn.style.display = 'none';
        retryBtn.style.display = 'inline-flex';
      });

      retryBtn.addEventListener('click', () => {
        userAssignments = {};
        selectedWord = null;
        updateScoreDisplay(0, maxScore);
        updateView();
      });
    }

    updateView();
    updateScoreDisplay(0, maxScore);
  }

  // Mark the Words Renderer
  function renderMarkTheWords(container, content) {
    const prompt = content.taskDescription || 'Click on all the correct words:';
    const text = content.textField || '';
    const regex = /\*([^*]+)\*/g;
    let targetWords = new Set();
    let match;

    while ((match = regex.exec(text)) !== null) {
      targetWords.add(match[1].trim());
    }

    maxScore = targetWords.size;
    const cleanText = text.replace(/\*([^*]+)\*/g, '$1');
    const tokens = cleanText.split(/(\s+)/);
    let selectedIndices = new Set();

    container.innerHTML = `
      <div class="h5p-question-prompt">${prompt}</div>
      <div style="line-height: 2.2; font-size: 1.1rem; margin-bottom: 20px;">
        ${tokens.map((token, i) => {
          if (token.trim().length === 0) return token;
          return `<span class="mtw-word" data-index="${i}" data-token="${token}" style="display: inline-block; padding: 2px 6px; border-radius: 6px; cursor: pointer; user-select: none;">${token}</span>`;
        }).join('')}
      </div>
      <div class="h5p-actions">
        <button class="h5p-btn h5p-btn-primary" id="mtw-check-btn">Check</button>
        <button class="h5p-btn h5p-btn-secondary" id="mtw-retry-btn" style="display: none;">Retry</button>
      </div>
    `;

    const wordEls = container.querySelectorAll('.mtw-word');
    wordEls.forEach(el => {
      el.addEventListener('click', () => {
        const idx = parseInt(el.getAttribute('data-index'));
        if (selectedIndices.has(idx)) {
          selectedIndices.delete(idx);
          el.style.background = 'transparent';
          el.style.border = 'none';
        } else {
          selectedIndices.add(idx);
          el.style.background = 'rgba(26, 95, 122, 0.15)';
          el.style.border = '1px solid var(--primary)';
        }
      });
    });

    const checkBtn = document.getElementById('mtw-check-btn');
    const retryBtn = document.getElementById('mtw-retry-btn');

    checkBtn.addEventListener('click', () => {
      let score = 0;
      wordEls.forEach(el => {
        const idx = parseInt(el.getAttribute('data-index'));
        const token = el.getAttribute('data-token').replace(/[.,\/#!$%\^&\*;:{}=\-_`~()]/g, '');
        const isTarget = targetWords.has(token);
        const isSelected = selectedIndices.has(idx);

        if (isSelected && isTarget) {
          score++;
          el.style.background = 'var(--success-bg)';
          el.style.border = '1.5px solid var(--success)';
          el.style.color = 'var(--success)';
          el.style.fontWeight = '700';
        } else if (isSelected && !isTarget) {
          el.style.background = 'var(--error-bg)';
          el.style.border = '1.5px solid var(--error)';
          el.style.color = 'var(--error)';
        } else if (!isSelected && isTarget) {
          el.style.border = '1.5px dashed var(--success)';
        }
      });

      updateScoreDisplay(score, maxScore);
      checkBtn.style.display = 'none';
      retryBtn.style.display = 'inline-flex';
    });

    retryBtn.addEventListener('click', () => {
      renderMarkTheWords(container, content);
      updateScoreDisplay(0, maxScore);
    });

    updateScoreDisplay(0, maxScore);
  }

  // Generic H5P package renderer fallback
  function renderGenericH5P(container, manifest, content) {
    container.innerHTML = `
      <div style="padding: 16px; background: rgba(0,0,0,0.02); border-radius: 8px;">
        <h3 style="margin-bottom: 8px;">${manifest.title || 'Interactive Activity'}</h3>
        <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 16px;">
          Library: <code>${manifest.mainLibrary || 'Custom H5P Package'}</code>
        </p>
        <pre style="background: var(--bg-color); padding: 12px; border-radius: 6px; overflow-x: auto; font-size: 0.8rem; color: var(--text-main);">
${JSON.stringify(content, null, 2)}
        </pre>
      </div>
    `;
    updateScoreDisplay(1, 1);
  }

  // Quiz (Question Set) Renderer - holds multiple MCQ questions with sequence navigation, dots, and results
  function renderQuestionSet(container, content) {
    const questions = content.questions || [];
    if (!questions.length) {
      container.innerHTML = '<div style="padding: 24px; text-align: center; color: var(--text-muted);">This quiz has no questions yet.</div>';
      updateScoreDisplay(0, 0);
      return;
    }

    let activeIndex = 0;
    const userSelections = questions.map(() => new Set());
    const questionStatus = questions.map(() => ({ checked: false, score: 0, maxScore: 1 }));

    // Precalculate max scores
    questions.forEach((q, idx) => {
      const params = q.params || q;
      const answers = params.answers || [];
      const singlePoint = params.behaviour ? params.behaviour.singlePoint !== false : true;
      const correctCount = answers.filter(a => a.correct).length;
      questionStatus[idx].maxScore = singlePoint ? 1 : Math.max(1, correctCount);
    });

    const totalMaxScore = questionStatus.reduce((acc, curr) => acc + curr.maxScore, 0);

    // Shuffle options per question so correct answers are randomized
    let shuffledAnswersPerQuestion = [];

    function shuffleOptions() {
      shuffledAnswersPerQuestion = questions.map(q => {
        const params = q.params || q;
        const rawAnswers = (params.answers || []).map((ans, origIdx) => ({
          ...ans,
          originalIndex: origIdx
        }));
        const shouldRandomize = params.behaviour ? params.behaviour.randomAnswers !== false : true;
        if (shouldRandomize && rawAnswers.length > 1) {
          const arr = [...rawAnswers];
          for (let i = arr.length - 1; i > 0; i--) {
            const j = Math.floor(Math.random() * (i + 1));
            [arr[i], arr[j]] = [arr[j], arr[i]];
          }
          return arr;
        }
        return rawAnswers;
      });
    }

    shuffleOptions();

    function getTotalScore() {
      return questionStatus.reduce((acc, curr) => acc + curr.score, 0);
    }

    function renderSummary() {
      const finalScore = getTotalScore();
      const pct = totalMaxScore > 0 ? Math.round((finalScore / totalMaxScore) * 100) : 0;
      const passPct = content.passPercentage !== undefined ? Number(content.passPercentage) : 50;
      const isPassed = pct >= passPct;

      updateScoreDisplay(finalScore, totalMaxScore);

      let breakdownHtml = '';
      questions.forEach((q, idx) => {
        const params = q.params || q;
        const qText = stripHtml(params.question || `Question ${idx + 1}`);
        const st = questionStatus[idx];
        const icon = st.score > 0 ? '✓' : '✗';
        const colorClass = st.score > 0 ? 'h5p-qs-item-pass' : 'h5p-qs-item-fail';
        breakdownHtml += `
          <div class="h5p-qs-breakdown-row ${colorClass}">
            <span class="h5p-qs-badge-icon">${icon}</span>
            <div class="h5p-qs-breakdown-text">
              <strong>Question ${idx + 1}:</strong> ${qText}
            </div>
            <span class="h5p-qs-breakdown-score">${st.score} / ${st.maxScore}</span>
          </div>
        `;
      });

      container.innerHTML = `
        <div class="h5p-qs-summary-card">
          <div class="h5p-qs-score-circle ${isPassed ? 'pass' : 'retry'}">
            <span class="h5p-qs-pct">${pct}%</span>
            <span class="h5p-qs-subscore">${finalScore} / ${totalMaxScore}</span>
          </div>
          <h2 class="h5p-qs-title">${isPassed ? 'Quiz Completed Successfully!' : 'Quiz Completed - Try Again!'}</h2>
          <p class="h5p-qs-subtitle">${isPassed ? 'Great job, you met the passing threshold!' : `Passing score is ${passPct}%. Keep practicing!`}</p>
          
          <div class="h5p-qs-breakdown-list">
            ${breakdownHtml}
          </div>

          <div class="h5p-actions" style="justify-content: center; margin-top: 24px;">
            <button class="h5p-btn h5p-btn-secondary" id="qs-review-btn">Review Questions</button>
            <button class="h5p-btn h5p-btn-primary" id="qs-retry-btn">Retry Quiz</button>
          </div>
        </div>
      `;

      document.getElementById('qs-retry-btn').addEventListener('click', () => {
        activeIndex = 0;
        questions.forEach((_, idx) => {
          userSelections[idx].clear();
          questionStatus[idx].checked = false;
          questionStatus[idx].score = 0;
        });
        shuffleOptions();
        updateScoreDisplay(0, totalMaxScore);
        renderCurrentQuestion();
      });

      document.getElementById('qs-review-btn').addEventListener('click', () => {
        activeIndex = 0;
        renderCurrentQuestion();
      });
    }

    function renderCurrentQuestion() {
      const q = questions[activeIndex];
      const params = q.params || q;
      const questionHtml = params.question || `Question ${activeIndex + 1}`;
      const answers = params.answers || [];
      const singlePoint = params.behaviour ? params.behaviour.singlePoint !== false : true;
      const inputType = singlePoint ? 'radio' : 'checkbox';
      const isChecked = questionStatus[activeIndex].checked;
      const selections = userSelections[activeIndex];

      notifyBridge('progress', { current: activeIndex + 1, total: questions.length });

      // Generate dots HTML
      let dotsHtml = '';
      questions.forEach((_, idx) => {
        let dotClass = 'h5p-qs-dot';
        if (idx === activeIndex) dotClass += ' active';
        if (questionStatus[idx].checked) {
          dotClass += (questionStatus[idx].score > 0) ? ' correct' : ' incorrect';
        }
        dotsHtml += `<button class="${dotClass}" data-index="${idx}" title="Go to question ${idx + 1}">${idx + 1}</button>`;
      });

      let optionsHtml = '';
      const displayedAnswers = shuffledAnswersPerQuestion[activeIndex] || [];
      displayedAnswers.forEach((ans) => {
        const origIdx = ans.originalIndex;
        const text = ans.text || '';
        const isSelected = selections.has(origIdx);
        let optClass = 'h5p-option';
        if (isSelected) optClass += ' selected';
        if (isChecked) {
          if (ans.correct) optClass += ' correct';
          else if (isSelected && !ans.correct) optClass += ' incorrect';
        }

        let feedbackHtml = '';
        if (isChecked && isSelected && ans.tipsAndFeedback && ans.tipsAndFeedback.chosenFeedback) {
          feedbackHtml = `<div class="h5p-feedback-msg">${ans.tipsAndFeedback.chosenFeedback}</div>`;
        }

        optionsHtml += `
          <label class="${optClass}" data-index="${origIdx}">
            <input type="${inputType}" name="qs-choice-${activeIndex}" value="${origIdx}" ${isSelected ? 'checked' : ''} ${isChecked ? 'disabled' : ''}>
            <span class="h5p-option-text">${text}</span>
            ${feedbackHtml}
          </label>
        `;
      });

      const isLastQuestion = activeIndex === questions.length - 1;

      container.innerHTML = `
        <div class="h5p-qs-container">
          <div class="h5p-qs-header-bar">
            <span class="h5p-qs-counter">Question ${activeIndex + 1} of ${questions.length}</span>
            <div class="h5p-qs-dots-bar">
              ${dotsHtml}
            </div>
          </div>

          <div class="h5p-question-prompt">${questionHtml}</div>
          <div class="h5p-options-list">${optionsHtml}</div>

          <div class="h5p-actions h5p-qs-nav">
            <div>
              ${activeIndex > 0 ? `<button class="h5p-btn h5p-btn-secondary" id="qs-prev-btn">← Previous</button>` : ''}
            </div>
            <div style="display: flex; gap: 8px;">
              ${!isChecked ? `<button class="h5p-btn h5p-btn-primary" id="qs-check-btn">Check</button>` : ''}
              ${isLastQuestion ? 
                `<button class="h5p-btn h5p-btn-primary" id="qs-finish-btn">Finish Quiz →</button>` : 
                `<button class="h5p-btn h5p-btn-secondary" id="qs-next-btn">Next →</button>`
              }
            </div>
          </div>
        </div>
      `;

      // Event handlers for dots navigation
      container.querySelectorAll('.h5p-qs-dot').forEach(dot => {
        dot.addEventListener('click', () => {
          const targetIdx = parseInt(dot.getAttribute('data-index'));
          activeIndex = targetIdx;
          renderCurrentQuestion();
        });
      });

      // Event handlers for selecting options
      if (!isChecked) {
        container.querySelectorAll('.h5p-option').forEach(el => {
          el.addEventListener('click', () => {
            const optIdx = parseInt(el.getAttribute('data-index'));
            if (singlePoint) {
              selections.clear();
              selections.add(optIdx);
              container.querySelectorAll('.h5p-option').forEach(o => o.classList.remove('selected'));
              el.classList.add('selected');
            } else {
              if (selections.has(optIdx)) {
                selections.delete(optIdx);
                el.classList.remove('selected');
              } else {
                selections.add(optIdx);
                el.classList.add('selected');
              }
            }
          });
        });
      }

      // Check button handler
      const checkBtn = document.getElementById('qs-check-btn');
      if (checkBtn) {
        checkBtn.addEventListener('click', () => {
          if (selections.size === 0) {
            alert('Please select an option before checking.');
            return;
          }
          questionStatus[activeIndex].checked = true;
          let earned = 0;
          answers.forEach((ans, idx) => {
            if (selections.has(idx) && ans.correct) earned++;
            else if (selections.has(idx) && !ans.correct && !singlePoint) earned = Math.max(0, earned - 1);
          });
          if (singlePoint) {
            questionStatus[activeIndex].score = earned > 0 ? 1 : 0;
          } else {
            questionStatus[activeIndex].score = earned;
          }
          updateScoreDisplay(getTotalScore(), totalMaxScore);
          renderCurrentQuestion();
        });
      }

      // Navigation buttons
      const prevBtn = document.getElementById('qs-prev-btn');
      if (prevBtn) {
        prevBtn.addEventListener('click', () => {
          if (activeIndex > 0) {
            activeIndex--;
            renderCurrentQuestion();
          }
        });
      }

      const nextBtn = document.getElementById('qs-next-btn');
      if (nextBtn) {
        nextBtn.addEventListener('click', () => {
          if (activeIndex < questions.length - 1) {
            activeIndex++;
            renderCurrentQuestion();
          }
        });
      }

      const finishBtn = document.getElementById('qs-finish-btn');
      if (finishBtn) {
        finishBtn.addEventListener('click', () => {
          renderSummary();
        });
      }
    }

    updateScoreDisplay(0, totalMaxScore);
    renderCurrentQuestion();
  }

  // Main loader exposed to Android
  window.loadH5P = function(manifestJsonStr, contentJsonStr) {
    try {
      currentManifest = typeof manifestJsonStr === 'string' ? JSON.parse(manifestJsonStr) : manifestJsonStr;
      currentContent = typeof contentJsonStr === 'string' ? JSON.parse(contentJsonStr) : contentJsonStr;

      // Update header
      const titleEl = document.getElementById('h5p-title');
      const typeEl = document.getElementById('h5p-type-badge');
      const descEl = document.getElementById('h5p-desc');
      const contentArea = document.getElementById('h5p-content');

      if (titleEl) titleEl.textContent = currentManifest.title || 'Interactive Activity';
      const mainLib = currentManifest.mainLibrary || '';
      if (typeEl) typeEl.textContent = mainLib.replace('H5P.', '');
      if (descEl && currentContent.description) {
        descEl.textContent = stripHtml(currentContent.description);
      }

      notifyBridge('loaded', { title: currentManifest.title, library: mainLib });

      // Route to appropriate content renderer
      if (mainLib.includes('QuestionSet') || (currentContent.questions && Array.isArray(currentContent.questions) && currentContent.questions.some(q => q.library || q.params))) {
        renderQuestionSet(contentArea, currentContent);
      } else if (mainLib.includes('TrueFalse') || currentContent.correct !== undefined) {
        renderTrueFalse(contentArea, currentContent);
      } else if (mainLib.includes('SingleChoiceSet') || currentContent.choices) {
        renderSingleChoiceSet(contentArea, currentContent);
      } else if (mainLib.includes('DragText')) {
        renderDragText(contentArea, currentContent);
      } else if (mainLib.includes('MarkTheWords')) {
        renderMarkTheWords(contentArea, currentContent);
      } else if (mainLib.includes('MultiChoice') || currentContent.answers) {
        renderMultiChoice(contentArea, currentContent);
      } else if (mainLib.includes('Dialogcards') || currentContent.dialogs) {
        renderDialogCards(contentArea, currentContent);
      } else if (mainLib.includes('Blanks') || currentContent.questions) {
        renderBlanks(contentArea, currentContent);
      } else {
        renderGenericH5P(contentArea, currentManifest, currentContent);
      }
    } catch (e) {
      console.error('Error loading H5P content:', e);
      const contentArea = document.getElementById('h5p-content');
      if (contentArea) {
        contentArea.innerHTML = `
          <div style="color: var(--error); padding: 20px;">
            <h4>Failed to load activity</h4>
            <p style="font-size: 0.9rem; margin-top: 6px;">${e.message}</p>
          </div>
        `;
      }
    }
  };

  // Signal ready
  document.addEventListener("DOMContentLoaded", () => {
    if (window.LumiBridge && window.LumiBridge.onRunnerReady) {
      window.LumiBridge.onRunnerReady();
    }
  });
})();
