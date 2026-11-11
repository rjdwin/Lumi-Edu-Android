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

    let html = `
      <div class="h5p-question-prompt">${questionHtml}</div>
      <div class="h5p-options-list" id="mc-options">
    `;

    answers.forEach((ans, idx) => {
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
      if (mainLib.includes('MultiChoice') || currentContent.answers) {
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
