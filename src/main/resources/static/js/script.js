const BASE_URL = "/api/gemini";
let currentQuiz = [];

// Ask
async function callAsk() {
    const input = document.getElementById("askInput").value;
    const res = await fetch(`${BASE_URL}/ask`, {
        method: "POST",
        headers: {"Content-Type": "text/plain"},
        body: input
    });
    const data = await res.text();
    document.getElementById("askOutput").textContent = data;
}

// Summarize
async function callSummarize() {
    const input = document.getElementById("summarizeInput").value;
    const res = await fetch(`${BASE_URL}/summarize`, {
        method: "POST",
        headers: {"Content-Type": "text/plain"},
        body: input
    });
    const data = await res.text();
    document.getElementById("summarizeOutput").textContent = data;
}

// Translate
async function callTranslate() {
    const input = document.getElementById("translateInput").value;
    const res = await fetch(`${BASE_URL}/translate`, {
        method: "POST",
        headers: {"Content-Type": "text/plain"},
        body: input
    });
    const data = await res.text();
    document.getElementById("translateOutput").textContent = data;
}

// Load Quiz
async function loadQuiz(type) {
    let topic;
    if(type === 'topic') topic = document.getElementById("quizInput").value;
    else topic = document.getElementById("quizTextInput").value;

    const endpoint = type === 'topic' ? 'quiz/topic-json' : 'quiz/text-json';

    const res = await fetch(`${BASE_URL}/${endpoint}`, {
        method: "POST",
        headers: {"Content-Type": "text/plain"},
        body: topic
    });

    const text = await res.text(); 
    let data = [];

    try {
        data = JSON.parse(text);
    } catch(e) {
        const start = text.indexOf('[');
        const end = text.lastIndexOf(']');
        if(start !== -1 && end !== -1){
            const jsonText = text.substring(start, end+1);
            try { data = JSON.parse(jsonText); } 
            catch(err) { alert("Failed to generate quiz."); return; }
        } else { alert("No JSON found."); return; }
    }

    currentQuiz = data;

    const form = document.getElementById(type==='topic'?"quizForm":"quizTextForm");
    form.innerHTML = "";
    data.forEach((q, i) => {
        const div = document.createElement("div");
        div.classList.add("mb-3");
        div.innerHTML = `<p><strong>${i+1}. ${q.question}</strong></p>`;
        q.options.forEach(opt => {
            div.innerHTML += `
                <div class="form-check">
                    <input class="form-check-input" type="radio" name="q${i}" value="${opt}" id="q${i}-${opt}">
                    <label class="form-check-label" for="q${i}-${opt}">${opt}</label>
                </div>`;
        });
        form.appendChild(div);
    });

    document.getElementById(type==='topic'?"submitQuizBtn":"submitQuizTextBtn").style.display = "inline-block";
    document.getElementById(type==='topic'?"quizResult":"quizTextResult").textContent = "";
}

function submitQuiz() {
    let score = 0;
    currentQuiz.forEach((q,i)=>{
        const selected = document.querySelector(`input[name="q${i}"]:checked`);
        if(selected && selected.value === q.answer) score++;
    });

    const resultEl = document.getElementById("quizResult");
    resultEl.textContent = `Your score: ${score} / ${currentQuiz.length}`;
    
    // Add class to show animation
    resultEl.classList.add('show');
    
    // Scroll result into view smoothly
    resultEl.scrollIntoView({ behavior: 'smooth', block: 'start' });

    // Remove animation class after short delay
    setTimeout(() => resultEl.classList.remove('show'), 300);
}



function submitQuizFromText() {
    let score = 0;
    currentQuizFromText.forEach((q, i) => {
        const selected = document.querySelector(`input[name="qt${i}"]:checked`);
        if(selected && selected.value === q.answer) score++;
    });

    const resultBox = document.getElementById("quizTextResult");
    resultBox.textContent = `Your score: ${score} / ${currentQuizFromText.length}`;
    
    // Add a quick pop animation
    resultBox.classList.add("show-score");
    setTimeout(() => resultBox.classList.remove("show-score"), 300);
}

