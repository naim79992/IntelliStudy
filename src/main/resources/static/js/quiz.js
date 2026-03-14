// js/quiz.js
let currentQuiz = [], submitted = false;

function setMode(mode) {
    ['topic','text','pdf'].forEach(m => {
        document.getElementById('mode-'+m).style.display = m===mode ? 'block' : 'none';
        document.getElementById('btn-'+m).classList.toggle('active', m===mode);
    });
    if (mode === 'pdf') checkPdfStatus();
    resetQuiz(false);
}

async function generateTopicQuiz() {
    const topic = document.getElementById('topicInput').value.trim();
    if (!topic) return;
    setLoading('topicBtn', true);
    try {
        const res = await fetch('/api/gemini/quiz/topic-json', { method: 'POST', headers: {'Content-Type':'text/plain'}, body: topic });
        const api = await res.json();
        const quizData = typeof api.data === 'string' ? JSON.parse(api.data) : api.data;
        renderQuiz(quizData);
    } catch(e) { alert('Failed to generate quiz'); }
    setLoading('topicBtn', false);
}

async function generateTextQuiz() {
    const text = document.getElementById('textInput').value.trim();
    if (!text) return;
    setLoading('textBtn', true);
    try {
        const res = await fetch('/api/gemini/quiz/text-json', { method: 'POST', headers: {'Content-Type':'text/plain'}, body: text });
        const api = await res.json();
        const quizData = typeof api.data === 'string' ? JSON.parse(api.data) : api.data;
        renderQuiz(quizData);
    } catch(e) { alert('Failed to generate quiz'); }
    setLoading('textBtn', false);
}

async function generatePdfQuiz() {
    setLoading('pdfBtn', true);
    try {
        const res = await fetch('/api/rag/quiz', { method: 'POST' });
        const api = await res.json();
        const quizData = typeof api.data === 'string' ? JSON.parse(api.data) : api.data;
        renderQuiz(quizData);
    } catch(e) { alert('Failed'); }
    setLoading('pdfBtn', false);
}

function renderQuiz(data) {
    currentQuiz = data; submitted = false;
    document.getElementById('quizContainer').style.display = 'block';
    document.getElementById('submitBtn').style.display = 'block';
    document.getElementById('scoreCard').innerHTML = '';
    const qDiv = document.getElementById('quizQuestions');
    qDiv.innerHTML = '';
    data.forEach((q, i) => {
        const card = document.createElement('div');
        card.className = 'quiz-card'; card.id = `qcard-${i}`;
        card.innerHTML = `
            <div class="quiz-q-num">Question ${i+1} of ${data.length}</div>
            <div class="quiz-question">${q.question}</div>
            <div class="quiz-options">
                ${q.options.map(opt => `
                    <div class="quiz-option" data-opt="${opt.replace(/"/g,'&quot;')}" onclick="selectOpt(this,${i})">
                        <input type="radio" name="q${i}" value="${opt.replace(/"/g,'&quot;')}">
                        <label>${opt}</label>
                    </div>`).join('')}
            </div>`;
        qDiv.appendChild(card);
    });
}

function selectOpt(el, qi) {
    document.querySelectorAll(`#qcard-${qi} .quiz-option`).forEach(o => o.style.background = '');
    el.querySelector('input').checked = true;
    el.style.background = 'rgba(255,107,53,0.06)';
}

function submitQuiz() {
    if (submitted) return; submitted = true;
    let score = 0;
    currentQuiz.forEach((q, i) => {
        const selected = document.querySelector(`input[name="q${i}"]:checked`);
        document.querySelectorAll(`#qcard-${i} .quiz-option`).forEach(opt => {
            const val = opt.dataset.opt;
            if (val === q.answer) opt.classList.add('correct');
            else if (selected && selected.value === val) opt.classList.add('wrong');
        });
        if (selected && selected.value === q.answer) score++;
    });
    const pct = Math.round((score / currentQuiz.length) * 100);
    const msg = pct >= 80 ? '🎉 Excellent!' : pct >= 50 ? '👍 Good effort!' : '📚 Keep studying!';
    document.getElementById('scoreCard').innerHTML = `
        <div class="score-card">
            <div class="score-num">${score}<span style="font-size:2.2rem;color:var(--text-muted)">/${currentQuiz.length}</span></div>
            <div class="score-label">${msg} You scored <strong>${pct}%</strong></div>
        </div>`;
    document.getElementById('submitBtn').style.display = 'none';
}

function resetQuiz() {
    currentQuiz = []; submitted = false;
    document.getElementById('quizContainer').style.display = 'none';
    document.getElementById('quizQuestions').innerHTML = '';
    document.getElementById('scoreCard').innerHTML = '';
}

function setLoading(id, on) {
    const btn = document.getElementById(id);
    document.getElementById('quizLoading').style.display = on ? 'block' : 'none';
    btn.disabled = on;
}

function selectOpt(el, qi) {
  document.querySelectorAll(`#qcard-${qi} .quiz-option`).forEach(o => o.style.background = '');
  el.querySelector('input').checked = true;
  el.style.background = 'rgba(255,107,53,0.06)';
}

function submitQuiz() {
  if (submitted) return; submitted = true;
  let score = 0;
  currentQuiz.forEach((q, i) => {
    const selected = document.querySelector(`input[name="q${i}"]:checked`);
    document.querySelectorAll(`#qcard-${i} .quiz-option`).forEach(opt => {
      const val = opt.dataset.opt;
      if (val === q.answer) opt.classList.add('correct');
      else if (selected && selected.value === val) opt.classList.add('wrong');
    });
    if (selected && selected.value === q.answer) score++;
  });
  const pct = Math.round((score / currentQuiz.length) * 100);
  const msg = pct >= 80 ? '🎉 Excellent!' : pct >= 50 ? '👍 Good effort!' : '📚 Keep studying!';
  document.getElementById('scoreCard').innerHTML = `
    <div class="score-card">
      <div class="score-num">${score}<span style="font-size:2.2rem;color:var(--text-muted)">/${currentQuiz.length}</span></div>
      <div class="score-label">${msg} You scored <strong>${pct}%</strong></div>
    </div>`;
  document.getElementById('submitBtn').style.display = 'none';
}

function resetQuiz(scroll = true) {
  currentQuiz = []; submitted = false;
  document.getElementById('quizContainer').style.display = 'none';
  document.getElementById('quizQuestions').innerHTML = '';
  document.getElementById('scoreCard').innerHTML = '';
}

function setLoading(id, on) {
  const btn = document.getElementById(id);
  document.getElementById('quizLoading').style.display = on ? 'block' : 'none';
  if (on) { btn.disabled = true; btn.style.opacity = '0.7'; }
  else { btn.disabled = false; btn.style.opacity = '1'; }
}