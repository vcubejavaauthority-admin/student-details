/**
 * Vcube Assistant - Session-Based AI Chat Widget
 * Supports multi-step conversation flows with state machine.
 * Includes data query flows (attendance by month/year/batch).
 * Uses Web Speech API for voice input.
 */
(function () {
    'use strict';

    const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    // ============ DOM SETUP ============
    const ASSISTANT_HTML = `
        <button id="vcube-assistant-fab" title="Vcube Assistant" aria-label="Open Vcube Assistant">
            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
            </svg>
        </button>

        <div id="vcube-assistant-panel">
            <div class="va-header">
                <div class="va-avatar">VA</div>
                <div class="va-header-info">
                    <h6>Vcube Assistant</h6>
                    <span>Online & Ready</span>
                </div>
            </div>
            <div class="va-messages" id="vaMessages"></div>
            <div class="va-input-area">
                <button class="va-btn va-btn-mic" id="vaMicBtn" title="Voice Input">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <path d="M12 2a3 3 0 0 0-3 3v7a3 3 0 0 0 6 0V5a3 3 0 0 0-3-3Z"></path>
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
                        <line x1="12" x2="12" y1="19" y2="22"></line>
                    </svg>
                </button>
                <input type="text" id="vaInput" placeholder="Type a message or use voice..." autocomplete="off">
                <button class="va-btn va-btn-send" id="vaSendBtn" title="Send Message">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                        <line x1="22" x2="11" y1="2" y2="13"></line>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
                    </svg>
                </button>
            </div>
        </div>
    `;

    const wrapper = document.createElement('div');
    wrapper.innerHTML = ASSISTANT_HTML;
    document.body.appendChild(wrapper);

    // ============ ELEMENTS ============
    const fab = document.getElementById('vcube-assistant-fab');
    const panel = document.getElementById('vcube-assistant-panel');
    const messagesDiv = document.getElementById('vaMessages');
    const input = document.getElementById('vaInput');
    const sendBtn = document.getElementById('vaSendBtn');
    const micBtn = document.getElementById('vaMicBtn');

    let isOpen = false;
    let isListening = false;
    let recognition = null;

    // ============ SESSION STATE MACHINE ============
    let session = { flow: null, step: null, data: {} };

    function resetSession() {
        session = { flow: null, step: null, data: {} };
    }

    // ============ SPEECH RECOGNITION ============
    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (SpeechRecognition) {
        recognition = new SpeechRecognition();
        recognition.continuous = false;
        recognition.interimResults = false;
        recognition.lang = 'en-US';

        recognition.onresult = function (event) {
            const transcript = event.results[0][0].transcript;
            input.value = transcript;
            stopListening();
            handleUserInput(transcript);
        };
        recognition.onerror = function (e) {
            stopListening();
            if (e.error !== 'no-speech') addMessage('bot', '🎤 Voice error: ' + e.error);
        };
        recognition.onend = function () { stopListening(); };
    }

    function startListening() {
        if (!recognition) { addMessage('bot', '🎤 Voice not supported. Use Chrome.'); return; }
        isListening = true;
        micBtn.classList.add('listening');
        recognition.start();
    }

    function stopListening() {
        isListening = false;
        micBtn.classList.remove('listening');
        try { recognition.stop(); } catch (e) { }
    }

    // ============ TOGGLE PANEL ============
    fab.addEventListener('click', () => {
        isOpen = !isOpen;
        panel.classList.toggle('open', isOpen);
        fab.classList.toggle('active', isOpen);
        if (isOpen && messagesDiv.children.length === 0) showMainMenu();
        if (isOpen) setTimeout(() => input.focus(), 400);
    });

    // ============ INPUT HANDLERS ============
    sendBtn.addEventListener('click', () => { const m = input.value.trim(); if (m) handleUserInput(m); });
    input.addEventListener('keydown', (e) => { if (e.key === 'Enter') { const m = input.value.trim(); if (m) handleUserInput(m); } });
    micBtn.addEventListener('click', () => { isListening ? stopListening() : startListening(); });

    // ============ MESSAGE HELPERS ============
    function addMessage(sender, text) {
        const div = document.createElement('div');
        div.className = 'va-msg ' + sender;
        if (sender === 'bot') {
            div.innerHTML = '<div class="msg-sender">Vcube Assistant</div>' + formatMessage(text);
        } else {
            div.textContent = text;
        }
        messagesDiv.appendChild(div);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
        return div;
    }

    function addHtmlMessage(html) {
        const div = document.createElement('div');
        div.className = 'va-msg bot';
        div.innerHTML = '<div class="msg-sender">Vcube Assistant</div>' + html;
        messagesDiv.appendChild(div);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
        return div;
    }

    function addQuickActions(actions) {
        const c = document.createElement('div');
        c.className = 'va-quick-actions';
        actions.forEach(a => {
            const btn = document.createElement('button');
            btn.className = 'va-quick-btn';
            btn.innerHTML = a.icon + ' ' + a.label;
            btn.addEventListener('click', () => {
                document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                addMessage('user', a.label);
                a.handler();
            });
            c.appendChild(btn);
        });
        messagesDiv.appendChild(c);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    function addBatchSelector(callback) {
        addTypingIndicator();
        fetch('/api/assistant/batches').then(r => r.json()).then(batches => {
            removeTypingIndicator();
            if (!batches || batches.length === 0) { addMessage('bot', '❌ No batches found.'); goHome(); return; }
            const c = document.createElement('div');
            c.className = 'va-quick-actions va-batch-grid';

            // Add "All Batches" option
            const allBtn = document.createElement('button');
            allBtn.className = 'va-quick-btn va-batch-btn all-batches-btn';
            allBtn.innerHTML = '<strong>🌐 All Batches</strong>';
            allBtn.style.gridColumn = '1 / -1';
            allBtn.addEventListener('click', () => {
                document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                addMessage('user', 'All Batches');
                callback('ALL');
            });
            c.appendChild(allBtn);

            batches.forEach(b => {
                const btn = document.createElement('button');
                btn.className = 'va-quick-btn va-batch-btn';
                btn.textContent = b;
                btn.addEventListener('click', () => {
                    document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                    addMessage('user', b);
                    callback(b);
                });
                c.appendChild(btn);
            });
            messagesDiv.appendChild(c);
            messagesDiv.scrollTop = messagesDiv.scrollHeight;
        }).catch(() => { removeTypingIndicator(); addMessage('bot', '❌ Error loading batches.'); goHome(); });
    }

    function addMonthSelector(callback) {
        const c = document.createElement('div');
        c.className = 'va-quick-actions va-month-grid';
        MONTHS.forEach((m, i) => {
            const btn = document.createElement('button');
            btn.className = 'va-quick-btn va-month-btn';
            btn.textContent = m.substring(0, 3);
            btn.addEventListener('click', () => {
                document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                addMessage('user', m);
                callback(i + 1, m);
            });
            c.appendChild(btn);
        });
        messagesDiv.appendChild(c);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    function addYearSelector(callback) {
        const currentYear = new Date().getFullYear();
        const c = document.createElement('div');
        c.className = 'va-quick-actions va-year-grid';
        for (let y = currentYear; y >= currentYear - 3; y--) {
            const btn = document.createElement('button');
            btn.className = 'va-quick-btn va-year-btn';
            btn.textContent = y;
            btn.addEventListener('click', () => {
                document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                addMessage('user', String(y));
                callback(y);
            });
            c.appendChild(btn);
        }
        messagesDiv.appendChild(c);
        messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    /**
     * Dynamic month/year selector — fetches available periods from the sheet.
     * Shows only months that actually have data.
     * @param {string} batch - Batch name
     * @param {string} sheetType - 'attendance', 'exam', or 'mock'
     * @param {function} callback - Called with (monthNum, year, label)
     */
    function addDynamicMonthSelector(batch, sheetType, callback) {
        addTypingIndicator();
        fetch(`/api/batch-available-dates?batch=${encodeURIComponent(batch)}&sheetType=${sheetType}`)
            .then(r => r.json())
            .then(data => {
                removeTypingIndicator();
                if (batch === 'ALL') {
                    // For All Batches, we show a simplified "Full History" option or use a global range
                    // Since fetching available dates for ALL batches might be slow, we provide a "Full History" button
                    addMessage('bot', '📅 Select a **report period**:');
                    const c = document.createElement('div');
                    c.className = 'va-quick-actions va-month-grid';
                    const fullHistoryBtn = document.createElement('button');
                    fullHistoryBtn.className = 'va-quick-btn va-month-btn';
                    fullHistoryBtn.style.gridColumn = '1 / -1';
                    fullHistoryBtn.innerHTML = '<strong>🕒 Full History (All Time)</strong>';
                    fullHistoryBtn.addEventListener('click', () => {
                        document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                        addMessage('user', 'Full History');
                        callback(null, null, 'Full History');
                    });
                    c.appendChild(fullHistoryBtn);
                    
                    // Also show current month/year from common dates if available
                    if (data.months && data.months.length > 0) {
                         data.months.slice(0, 8).forEach(item => {
                            const btn = document.createElement('button');
                            btn.className = 'va-quick-btn va-month-btn';
                            const shortMonth = item.label.split(' ')[0].substring(0, 3);
                            const yr = item.label.split(' ')[1];
                            btn.textContent = shortMonth + ' ' + yr;
                            btn.addEventListener('click', () => {
                                document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                                addMessage('user', item.label);
                                callback(item.month, item.year, item.label);
                            });
                            c.appendChild(btn);
                        });
                    }
                    messagesDiv.appendChild(c);
                    messagesDiv.scrollTop = messagesDiv.scrollHeight;
                    return;
                }

                if (!data.months || data.months.length === 0) {
                    addMessage('bot', '❌ No data found for this batch. No dates available in the sheet.');
                    goHome();
                    return;
                }
                addMessage('bot', '📆 Select a **month/year** period:');
                const c = document.createElement('div');
                c.className = 'va-quick-actions va-month-grid';
                data.months.forEach(item => {
                    const btn = document.createElement('button');
                    btn.className = 'va-quick-btn va-month-btn';
                    const shortMonth = item.label.split(' ')[0].substring(0, 3);
                    const yr = item.label.split(' ')[1];
                    btn.textContent = shortMonth + ' ' + yr;
                    btn.addEventListener('click', () => {
                        document.querySelectorAll('.va-quick-actions').forEach(el => el.remove());
                        addMessage('user', item.label);
                        callback(item.month, item.year, item.label);
                    });
                    c.appendChild(btn);
                });
                messagesDiv.appendChild(c);
                messagesDiv.scrollTop = messagesDiv.scrollHeight;
            })
            .catch(() => {
                removeTypingIndicator();
                addMessage('bot', '❌ Error loading available dates.');
                goHome();
            });
    }

    function addTypingIndicator() {
        const d = document.createElement('div'); d.className = 'va-typing'; d.id = 'vaTyping';
        d.innerHTML = '<span></span><span></span><span></span>';
        messagesDiv.appendChild(d); messagesDiv.scrollTop = messagesDiv.scrollHeight;
    }

    function removeTypingIndicator() { const el = document.getElementById('vaTyping'); if (el) el.remove(); }

    function formatMessage(text) {
        return text.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br>');
    }

    // ============ MAIN MENU ============
    function showMainMenu() {
        addMessage('bot', '👋 Hi! I\'m **Vcube Assistant**.\n\nWhat would you like to do?');
        addQuickActions([
            { icon: '✅', label: 'Mark Attendance', handler: () => startFlow('mark-attendance') },
            { icon: '📝', label: 'Add Exam Marks', handler: () => startFlow('exam-marks') },
            { icon: '🎯', label: 'Add Mock Marks', handler: () => startFlow('mock-marks') },
            { icon: '📋', label: 'Add Case-Study', handler: () => startFlow('casestudy-marks') },
            { icon: '📊', label: 'Attendance Report', handler: () => startFlow('view-attendance') },
            { icon: '📈', label: 'Exam Report', handler: () => startFlow('view-exam') },
            { icon: '🎯', label: 'Mock Report', handler: () => startFlow('view-mock') },
            { icon: '📅', label: 'Monthly Attendance', handler: () => startFlow('monthly-report') },
            { icon: '📝', label: 'Monthly Exam', handler: () => startFlow('exam-monthly-report') },
            { icon: '🎯', label: 'Monthly Mock', handler: () => startFlow('mock-monthly-report') },
            { icon: '📚', label: 'Show Batches', handler: () => startFlow('show-batches') },
            { icon: '👤', label: 'Student Count', handler: () => startFlow('student-count') }
        ]);
    }

    function goHome() {
        resetSession();
        setTimeout(() => {
            addMessage('bot', '🏠 **Back to Home Menu**\n\nWhat else would you like to do?');
            addQuickActions([
                { icon: '✅', label: 'Mark Attendance', handler: () => startFlow('mark-attendance') },
                { icon: '📝', label: 'Exam Marks', handler: () => startFlow('exam-marks') },
                { icon: '🎯', label: 'Mock Marks', handler: () => startFlow('mock-marks') },
                { icon: '📊', label: 'Reports', handler: () => startFlow('view-attendance') },
                { icon: '📅', label: 'Monthly Attendance', handler: () => startFlow('monthly-report') },
                { icon: '📝', label: 'Monthly Exam', handler: () => startFlow('exam-monthly-report') },
                { icon: '🎯', label: 'Monthly Mock', handler: () => startFlow('mock-monthly-report') },
                { icon: '📚', label: 'Batches', handler: () => startFlow('show-batches') }
            ]);
        }, 600);
    }

    // ============ FLOW ROUTER ============
    function startFlow(flow) {
        resetSession();
        session.flow = flow;

        switch (flow) {
            case 'mark-attendance':
                session.step = 'ask-batch';
                addMessage('bot', '✅ **Mark Attendance**\n\nSelect the batch:');
                addBatchSelector(b => { session.data.batch = b; session.step = 'ask-date'; askForDate(); });
                break;

            case 'exam-marks':
                session.step = 'ask-batch';
                addMessage('bot', '📝 **Add Exam Marks**\n\nSelect the batch:');
                addBatchSelector(b => { session.data.batch = b; session.step = 'ask-date'; askForDate(); });
                break;

            case 'mock-marks':
                session.step = 'ask-batch';
                addMessage('bot', '🎯 **Add Mock Marks**\n\nSelect the batch:');
                addBatchSelector(b => { session.data.batch = b; session.step = 'ask-date'; askForDate(); });
                break;

            case 'casestudy-marks':
                session.step = 'ask-batch';
                addMessage('bot', '📋 **Add Case-Study**\n\nSelect the batch:');
                addBatchSelector(b => { session.data.batch = b; session.step = 'ask-date'; askForDate(); });
                break;

            case 'view-attendance':
                session.step = 'ask-batch';
                addMessage('bot', '📊 **Attendance Report**\n\nSelect the batch:');
                addBatchSelector(b => { fetchAndShowReport('attendance', b); });
                break;

            case 'view-exam':
                session.step = 'ask-batch';
                addMessage('bot', '📈 **Exam Report**\n\nSelect the batch:');
                addBatchSelector(b => { fetchAndShowReport('exam', b); });
                break;

            case 'view-mock':
                session.step = 'ask-batch';
                addMessage('bot', '🎯 **Mock Report**\n\nSelect the batch:');
                addBatchSelector(b => { fetchAndShowReport('mock', b); });
                break;

            // ── Monthly Attendance Report Flow (dynamic months from sheet) ──
            case 'monthly-report':
                session.step = 'ask-batch';
                addMessage('bot', '📅 **Monthly Attendance Report**\n\nSelect the batch:');
                addBatchSelector(b => {
                    session.data.batch = b;
                    session.step = 'ask-month-year';
                    addDynamicMonthSelector(b, 'attendance', (monthNum, year, label) => {
                        session.data.month = monthNum;
                        session.data.year = year;
                        session.data.monthName = label.split(' ')[0];
                        fetchMonthlyReport(b, monthNum, year, session.data.monthName);
                    });
                });
                break;

            // ── Monthly Exam Report Flow ──
            case 'exam-monthly-report':
                session.step = 'ask-batch';
                addMessage('bot', '📝 **Monthly Exam Report**\n\nSelect the batch:');
                addBatchSelector(b => {
                    session.data.batch = b;
                    session.step = 'ask-month-year';
                    addDynamicMonthSelector(b, 'exam', (monthNum, year, label) => {
                        fetchMonthlyMarksReport(b, 'exam', monthNum, year, label);
                    });
                });
                break;

            // ── Monthly Mock Report Flow ──
            case 'mock-monthly-report':
                session.step = 'ask-batch';
                addMessage('bot', '🎯 **Monthly Mock Report**\n\nSelect the batch:');
                addBatchSelector(b => {
                    session.data.batch = b;
                    session.step = 'ask-month-year';
                    addDynamicMonthSelector(b, 'mock', (monthNum, year, label) => {
                        fetchMonthlyMarksReport(b, 'mock', monthNum, year, label);
                    });
                });
                break;

            case 'show-batches':
                addTypingIndicator();
                fetch('/api/assistant/batches').then(r => r.json()).then(batches => {
                    removeTypingIndicator();
                    if (batches && batches.length > 0) {
                        addMessage('bot', '📚 **All Available Batches:**\n\n' + batches.map(b => '• **' + b + '**').join('\n'));
                    } else {
                        addMessage('bot', '❌ No batches found.');
                    }
                    goHome();
                }).catch(() => { removeTypingIndicator(); addMessage('bot', '❌ Error.'); goHome(); });
                break;

            case 'student-count':
                session.step = 'ask-batch';
                addMessage('bot', '👤 **Student Count**\n\nSelect the batch:');
                addBatchSelector(b => { fetchStudentCount(b); });
                break;

            default:
                goHome();
        }
    }

    // ============ DATA QUERY: MONTHLY ATTENDANCE REPORT ============
    function fetchMonthlyReport(batch, month, year, monthName) {
        addTypingIndicator();

        fetch(`/api/batch-attendance-report?batch=${encodeURIComponent(batch)}${month ? `&month=${month}&year=${year}` : ''}`)
            .then(r => r.json())
            .then(data => {
                removeTypingIndicator();

                if (Array.isArray(data)) {
                    // Aggregated report
                    renderAggregatedAttendance(data, monthName);
                    return;
                }

                if (!data || data.totalStudents === 0) {
                    addMessage('bot', '📊 No attendance data found for **' + batch + '** in **' + monthName + ' ' + year + '**.');
                    goHome();
                    return;
                }

                // Summary card HTML
                const total = data.totalStudents;
                const regular = data.regularCount;
                const irregular = data.irregularCount;
                const days = data.totalWorkingDays;
                const regPct = total > 0 ? Math.round(regular / total * 100) : 0;
                const irrPct = total > 0 ? Math.round(irregular / total * 100) : 0;

                let html = `<strong>📅 Monthly Report — ${batch} — ${monthName} ${year}</strong><br><br>`;

                html += `<div class="va-summary-card">
                    <div class="va-stat"><div class="va-stat-label">Total Students</div><div class="va-stat-value info">${total}</div></div>
                    <div class="va-stat"><div class="va-stat-label">Working Days</div><div class="va-stat-value warning">${days}</div></div>
                    <div class="va-stat"><div class="va-stat-label">Regular (≥75%)</div><div class="va-stat-value success">${regular} <small>(${regPct}%)</small></div></div>
                    <div class="va-stat"><div class="va-stat-label">Irregular (&lt;75%)</div><div class="va-stat-value danger">${irregular} <small>(${irrPct}%)</small></div></div>
                </div>`;

                addHtmlMessage(html);

                // Student table
                if (data.students && data.students.length > 0) {
                    let tableHtml = '<strong>📋 Student Details</strong><br>';
                    tableHtml += '<table class="va-data-table"><thead><tr>';
                    tableHtml += '<th>#</th><th>Name</th><th>P</th><th>A</th><th>%</th><th>Status</th>';
                    tableHtml += '</tr></thead><tbody>';

                    const max = Math.min(data.students.length, 15);
                    for (let i = 0; i < max; i++) {
                        const s = data.students[i];
                        const pct = s.attendancePercentage;
                        let pctClass = 'va-pct-high';
                        if (pct < 50) pctClass = 'va-pct-low';
                        else if (pct < 75) pctClass = 'va-pct-mid';

                        const statusClass = s.regular ? 'va-status-regular' : 'va-status-irregular';
                        const statusText = s.regular ? '✓ Regular' : '✗ Irregular';

                        tableHtml += `<tr>
                            <td>${i + 1}</td>
                            <td>${s.name}</td>
                            <td>${s.presentDays}</td>
                            <td>${s.absentDays}</td>
                            <td><span class="va-pct ${pctClass}">${pct}%</span></td>
                            <td><span class="${statusClass}">${statusText}</span></td>
                        </tr>`;
                    }

                    tableHtml += '</tbody></table>';

                    if (data.students.length > 15) {
                        tableHtml += `<br><em>Showing 15 of ${data.students.length} students</em>`;
                    }

                    addHtmlMessage(tableHtml);
                }

                goHome();
            })
            .catch(err => {
                removeTypingIndicator();
                addMessage('bot', '❌ Error fetching monthly report. Please try again.');
                goHome();
            });
    }

    // ============ MONTHLY MARKS REPORT FETCHER ============
    function fetchMonthlyMarksReport(batch, sheetType, month, year, label) {
        addTypingIndicator();
        const apiUrl = sheetType === 'exam' ? '/api/batch-exam-report' : '/api/batch-mock-report';
        const typeLabel = sheetType === 'exam' ? 'Exam' : 'Mock';
        const typeIcon = sheetType === 'exam' ? '📝' : '🎯';

        fetch(`${apiUrl}?batch=${encodeURIComponent(batch)}${month ? `&month=${month}&year=${year}` : ''}`)
            .then(r => r.json())
            .then(data => {
                removeTypingIndicator();

                if (Array.isArray(data)) {
                    renderAggregatedMarks(data, typeLabel, label);
                    return;
                }

                if (!data || data.totalStudents === 0) {
                    addMessage('bot', `${typeIcon} No ${typeLabel.toLowerCase()} data found for **${batch}** in **${label}**.`);
                    goHome();
                    return;
                }

                const total = data.totalStudents;
                const pass = data.passCount || 0;
                const fail = data.failCount || 0;
                const exams = data.totalExams || 0;
                const avg = data.averagePercentage || 0;
                const passPct = total > 0 ? Math.round(pass / total * 100) : 0;
                const failPct = total > 0 ? Math.round(fail / total * 100) : 0;

                let html = `<strong>${typeIcon} ${typeLabel} Report — ${batch} — ${label}</strong><br><br>`;
                html += `<div class="va-summary-card">
                    <div class="va-stat"><div class="va-stat-label">Total Students</div><div class="va-stat-value info">${total}</div></div>
                    <div class="va-stat"><div class="va-stat-label">${typeLabel}s Conducted</div><div class="va-stat-value warning">${exams}</div></div>
                    <div class="va-stat"><div class="va-stat-label">Pass (≥50%)</div><div class="va-stat-value success">${pass} <small>(${passPct}%)</small></div></div>
                    <div class="va-stat"><div class="va-stat-label">Fail (&lt;50%)</div><div class="va-stat-value danger">${fail} <small>(${failPct}%)</small></div></div>
                </div>`;

                addHtmlMessage(html);

                // Student table
                const students = data.students;
                if (students && students.length > 0) {
                    let tbl = `<strong>📋 ${typeLabel} Details</strong><br>`;
                    tbl += '<table class="va-data-table"><thead><tr>';
                    tbl += '<th>#</th><th>Name</th><th>Attended</th><th>Marks</th><th>%</th><th>Status</th>';
                    tbl += '</tr></thead><tbody>';

                    const max = Math.min(students.length, 15);
                    for (let i = 0; i < max; i++) {
                        const s = students[i];
                        const pct = s.percentage || 0;
                        let pctClass = pct >= 50 ? 'va-pct-high' : pct >= 35 ? 'va-pct-mid' : 'va-pct-low';
                        const statusCls = s.passed ? 'va-status-regular' : 'va-status-irregular';
                        const statusTxt = s.passed ? '✓ Pass' : '✗ Fail';

                        tbl += `<tr>
                            <td>${i + 1}</td>
                            <td>${s.name || s.rollNo}</td>
                            <td>${s.attended}/${s.totalExams}</td>
                            <td>${s.totalMarks}</td>
                            <td><span class="va-pct ${pctClass}">${pct}%</span></td>
                            <td><span class="${statusCls}">${statusTxt}</span></td>
                        </tr>`;
                    }
                    tbl += '</tbody></table>';
                    if (students.length > 15) tbl += `<br><em>Showing 15 of ${students.length}</em>`;
                    addHtmlMessage(tbl);
                }

                goHome();
            })
            .catch(err => {
                removeTypingIndicator();
                addMessage('bot', `❌ Error fetching ${typeLabel.toLowerCase()} report. Please try again.`);
                goHome();
            });
    }

    // ============ STEP HANDLERS ============
    function askForDate() {
        const today = new Date();
        const dd = String(today.getDate()).padStart(2, '0');
        const mm = String(today.getMonth() + 1).padStart(2, '0');
        const yyyy = today.getFullYear();
        const todayStr = dd + '-' + mm + '-' + yyyy;
        addMessage('bot', '📅 **Enter the date** (dd-mm-yyyy)\n\nOr type **"today"** for ' + todayStr);
        session.step = 'waiting-date';
    }

    function askForRollNumbers() {
        if (session.flow === 'mark-attendance') {
            addMessage('bot', '📋 **Enter roll numbers** (comma-separated)\n\nExample: **1, 2, 3, 5, 7**');
            session.step = 'waiting-rolls';
        } else {
            addMessage('bot', '📋 **Enter roll numbers with marks**\n\nFormat: **roll=marks**\nExample: **1=85, 2=90, 3=78**');
            session.step = 'waiting-roll-marks';
        }
    }

    function confirmAndSubmit() {
        const d = session.data;
        let summary = '';

        if (session.flow === 'mark-attendance') {
            summary = '✅ **Confirm Attendance**\n\n📚 Batch: **' + d.batch + '**\n📅 Date: **' + d.date + '**\n📋 Rolls: **' + d.rollNos + '**\n\nType **"yes"** to confirm or **"no"** to cancel.';
        } else {
            const label = session.flow === 'exam-marks' ? 'Exam' : session.flow === 'mock-marks' ? 'Mock' : 'Case-Study';
            summary = '📝 **Confirm ' + label + ' Marks**\n\n📚 Batch: **' + d.batch + '**\n📅 Date: **' + d.date + '**\n📋 Data: **' + d.rollMarks + '**\n\nType **"yes"** to confirm or **"no"** to cancel.';
        }

        addMessage('bot', summary);
        session.step = 'waiting-confirm';
    }

    function executeSubmit() {
        const d = session.data;
        addTypingIndicator();

        let url = '', payload = {};
        if (session.flow === 'mark-attendance') {
            url = '/api/assistant/mark-attendance';
            payload = { batch: d.batch, rollNos: d.rollNos, date: d.date };
        } else if (session.flow === 'exam-marks') {
            url = '/api/assistant/add-exam-marks';
            payload = { batch: d.batch, rollMarks: d.rollMarks, date: d.date };
        } else if (session.flow === 'mock-marks') {
            url = '/api/assistant/add-mock-marks';
            payload = { batch: d.batch, rollMarks: d.rollMarks, date: d.date };
        } else if (session.flow === 'casestudy-marks') {
            url = '/api/assistant/add-casestudy-marks';
            payload = { batch: d.batch, rollMarks: d.rollMarks, date: d.date };
        }

        fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
            .then(r => r.json())
            .then(result => {
                removeTypingIndicator();
                if (result.success) {
                    let msg = '🎉 **Success!**\n\n' + (result.message || 'Operation completed.');
                    if (result.invalidRolls && result.invalidRolls.length > 0) {
                        msg += '\n\n⚠️ **Invalid Rolls:** ' + result.invalidRolls.join(', ');
                    }
                    addMessage('bot', msg);
                } else {
                    addMessage('bot', '❌ **Failed:** ' + (result.message || 'Something went wrong.'));
                }
                goHome();
            })
            .catch(() => { removeTypingIndicator(); addMessage('bot', '❌ Connection error.'); goHome(); });
    }

    // ============ REPORT FETCHERS ============
    function fetchAndShowReport(type, batch) {
        addTypingIndicator();
        let url = '';
        if (type === 'attendance') url = '/api/assistant/attendance-report?batch=' + batch;
        else if (type === 'exam') url = '/api/assistant/exam-report?batch=' + batch;
        else if (type === 'mock') url = '/api/assistant/mock-report?batch=' + batch;

        fetch(url).then(r => r.json()).then(data => {
            removeTypingIndicator();
            if (!data.success && data.message) { addMessage('bot', '❌ ' + data.message); goHome(); return; }
            addHtmlMessage(formatReportData(data));
            goHome();
        }).catch(() => { removeTypingIndicator(); addMessage('bot', '❌ Error fetching report.'); goHome(); });
    }

    function fetchStudentCount(batch) {
        addTypingIndicator();
        fetch('/api/assistant/attendance-report?batch=' + batch).then(r => r.json()).then(data => {
            removeTypingIndicator();
            const count = data.totalStudents || 0;
            addMessage('bot', '👤 Batch **' + batch + '** has **' + count + '** students.');
            goHome();
        }).catch(() => { removeTypingIndicator(); addMessage('bot', '❌ Error.'); goHome(); });
    }

    function formatReportData(data) {
        const students = data.students;
        if (!students || students.length === 0) return '📊 No data found for batch <strong>' + data.batch + '</strong>.';

        const isAll = data.batch === 'ALL';
        let html = '<strong>📊 ' + capitalize(data.type) + ' Report — ' + (isAll ? 'All Batches' : 'Batch ' + data.batch) + '</strong><br>';
        html += 'Total students: <strong>' + data.totalStudents + '</strong><br><br>';

        html += '<table class="va-data-table"><thead><tr>';
        if (isAll) html += '<th>Batch</th>';
        html += '<th>Roll No</th><th>Name</th>';
        if (data.type === 'exam' || data.type === 'mock') html += '<th>Marks</th><th>%</th>';
        else if (data.type === 'casestudy') html += '<th>Attended</th>';
        html += '</tr></thead><tbody>';

        const max = Math.min(students.length, 20);
        for (let i = 0; i < max; i++) {
            const s = students[i];
            html += '<tr>';
            if (isAll) html += '<td>' + (s.batch || '') + '</td>';
            html += '<td>' + (s.rollNo || '') + '</td><td>' + (s.name || '') + '</td>';
            if (data.type === 'exam' || data.type === 'mock') {
                const pct = s.percentage || 0;
                let cls = 'va-pct-high';
                if (pct < 50) cls = 'va-pct-low';
                else if (pct < 75) cls = 'va-pct-mid';
                html += '<td>' + (s.totalMarks || 0) + '</td><td><span class="va-pct ' + cls + '">' + pct + '%</span></td>';
            } else if (data.type === 'casestudy') {
                html += '<td>' + (s.attended || 0) + '/' + (s.totalCaseStudies || 0) + '</td>';
            }
            html += '</tr>';
        }
        html += '</tbody></table>';
        if (students.length > max) html += '<br><em>Showing ' + max + ' of ' + students.length + ' students</em>';
        return html;
    }

    function renderAggregatedAttendance(reports, period) {
        let totalStudents = 0, totalRegular = 0, totalIrregular = 0;
        reports.forEach(r => {
            totalStudents += r.totalStudents;
            totalRegular += r.regularCount;
            totalIrregular += r.irregularCount;
        });

        const regPct = totalStudents > 0 ? Math.round(totalRegular / totalStudents * 100) : 0;

        let html = `<strong>🌐 All Batches Attendance — ${period}</strong><br><br>`;
        html += `<div class="va-summary-card">
            <div class="va-stat"><div class="va-stat-label">Total Students</div><div class="va-stat-value info">${totalStudents}</div></div>
            <div class="va-stat"><div class="va-stat-label">Regular Overall</div><div class="va-stat-value success">${totalRegular} <small>(${regPct}%)</small></div></div>
            <div class="va-stat"><div class="va-stat-label">Irregular Overall</div><div class="va-stat-value danger">${totalIrregular}</div></div>
        </div><br>`;

        html += '<strong>📋 Batch Breakdown</strong><br>';
        html += '<table class="va-data-table"><thead><tr><th>Batch</th><th>Total</th><th>Regular</th><th>%</th></tr></thead><tbody>';
        reports.forEach(r => {
            const p = r.totalStudents > 0 ? Math.round(r.regularCount / r.totalStudents * 100) : 0;
            html += `<tr><td>${r.batch}</td><td>${r.totalStudents}</td><td>${r.regularCount}</td><td>${p}%</td></tr>`;
        });
        html += '</tbody></table>';
        addHtmlMessage(html);
        goHome();
    }

    function renderAggregatedMarks(reports, typeLabel, period) {
        let totalStudents = 0, totalPass = 0, totalFail = 0;
        reports.forEach(r => {
            totalStudents += r.totalStudents;
            totalPass += r.passCount;
            totalFail += r.failCount;
        });

        const passPct = totalStudents > 0 ? Math.round(totalPass / totalStudents * 100) : 0;

        let html = `<strong>🌐 All Batches ${typeLabel}s — ${period}</strong><br><br>`;
        html += `<div class="va-summary-card">
            <div class="va-stat"><div class="va-stat-label">Total Students</div><div class="va-stat-value info">${totalStudents}</div></div>
            <div class="va-stat"><div class="va-stat-label">Passed Overall</div><div class="va-stat-value success">${totalPass} <small>(${passPct}%)</small></div></div>
            <div class="va-stat"><div class="va-stat-label">Failed Overall</div><div class="va-stat-value danger">${totalFail}</div></div>
        </div><br>`;

        html += '<strong>📋 Batch Breakdown</strong><br>';
        html += '<table class="va-data-table"><thead><tr><th>Batch</th><th>Total</th><th>Pass</th><th>%</th></tr></thead><tbody>';
        reports.forEach(r => {
            const p = r.totalStudents > 0 ? Math.round(r.passCount / r.totalStudents * 100) : 0;
            html += `<tr><td>${r.batch}</td><td>${r.totalStudents}</td><td>${r.passCount}</td><td>${p}%</td></tr>`;
        });
        html += '</tbody></table>';
        addHtmlMessage(html);
        goHome();
    }

    function capitalize(str) {
        if (str === 'casestudy') return 'Case Study';
        return str.charAt(0).toUpperCase() + str.slice(1);
    }

    // ============ MAIN INPUT PROCESSOR ============
    function handleUserInput(text) {
        input.value = '';
        addMessage('user', text);
        const msg = text.toLowerCase().trim();

        // Cancel / Home always works
        if (['cancel', 'home', 'menu', 'back', 'exit'].includes(msg)) {
            addMessage('bot', '👍 Returning to home menu.');
            goHome();
            return;
        }

        // Active session → route to step handler
        if (session.flow && session.step) {
            handleFlowStep(msg, text);
            return;
        }

        // Check for explicit button-style commands first (mark attendance etc.)
        if (containsAny(msg, 'mark attendance') && !containsAny(msg, 'show', 'view', 'get', 'report')) {
            startFlow('mark-attendance'); return;
        }
        if (containsAny(msg, 'add exam') && !containsAny(msg, 'show', 'view', 'get', 'report')) {
            startFlow('exam-marks'); return;
        }
        if (containsAny(msg, 'add mock') && !containsAny(msg, 'show', 'view', 'get', 'report')) {
            startFlow('mock-marks'); return;
        }

        // For EVERYTHING else → send to smart backend for NLP analysis
        sendSmartQuery(text);
    }

    /**
     * Send any free-text query to the smart backend /api/assistant/chat
     * The backend extracts batch, data type, time period, filter and returns structured data
     */
    function sendSmartQuery(text) {
        addTypingIndicator();

        fetch('/api/assistant/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: text })
        })
            .then(r => r.json())
            .then(data => {
                removeTypingIndicator();
                renderSmartResponse(data);
            })
            .catch(err => {
                removeTypingIndicator();
                addMessage('bot', '❌ Connection error. Server might be down.');
            });
    }

    /**
     * Render the smart response based on type
     */
    function renderSmartResponse(data) {
        const type = data.type;

        switch (type) {
            case 'smart-attendance':
                renderSmartAttendance(data);
                break;
            case 'smart-exam':
                renderSmartExam(data);
                break;
            case 'smart-mock':
                renderSmartMock(data);
                break;
            case 'smart-casestudy':
                renderSmartCaseStudy(data);
                break;
            case 'start-flow':
                // Backend says start a write flow
                if (data.action) startFlow(data.action);
                else addMessage('bot', data.reply || 'Starting flow...');
                break;
            case 'help':
            case 'info':
            case 'ask':
            case 'error':
                addMessage('bot', data.reply || 'No response.');
                break;
            default:
                if (data.reply) addMessage('bot', data.reply);
                else addMessage('bot', '🤔 I couldn\'t process that. Try **"menu"** for options.');
        }
    }

    // ═══════════════════════════════════════
    // Smart Attendance Renderer
    // ═══════════════════════════════════════
    function renderSmartAttendance(data) {
        const batch = data.batch;
        const period = data.period || 'Current Month';
        const filterLabel = data.filterLabel || null;
        const total = data.totalStudents;
        const regular = data.regularCount;
        const irregular = data.irregularCount;
        const days = data.totalWorkingDays;
        const regPct = total > 0 ? Math.round(regular / total * 100) : 0;
        const irrPct = total > 0 ? Math.round(irregular / total * 100) : 0;

        // Title
        let titleHtml = `<strong>📊 Attendance — ${batch} — ${period}</strong>`;
        if (filterLabel) titleHtml += `<br><span style="color: var(--va-accent); font-size: 0.75rem;">🔍 ${filterLabel}</span>`;
        titleHtml += '<br><br>';

        // Summary cards
        titleHtml += `<div class="va-summary-card">
            <div class="va-stat"><div class="va-stat-label">Total Students</div><div class="va-stat-value info">${total}</div></div>
            <div class="va-stat"><div class="va-stat-label">Working Days</div><div class="va-stat-value warning">${days}</div></div>
            <div class="va-stat"><div class="va-stat-label">Regular (≥75%)</div><div class="va-stat-value success">${regular} <small>(${regPct}%)</small></div></div>
            <div class="va-stat"><div class="va-stat-label">Irregular (&lt;75%)</div><div class="va-stat-value danger">${irregular} <small>(${irrPct}%)</small></div></div>
        </div>`;

        addHtmlMessage(titleHtml);

        // Student table
        const students = data.students;
        if (students && students.length > 0) {
            let tbl = '<strong>📋 Student Details</strong><br>';
            tbl += '<table class="va-data-table"><thead><tr>';
            tbl += '<th>#</th><th>Name</th><th>P</th><th>A</th><th>%</th><th>Status</th>';
            tbl += '</tr></thead><tbody>';

            const max = Math.min(students.length, 15);
            for (let i = 0; i < max; i++) {
                const s = students[i];
                const pct = s.percentage || 0;
                let pctClass = pct >= 75 ? 'va-pct-high' : pct >= 50 ? 'va-pct-mid' : 'va-pct-low';
                const statusCls = s.regular ? 'va-status-regular' : 'va-status-irregular';
                const statusTxt = s.regular ? '✓ Regular' : '✗ Irregular';

                tbl += `<tr>
                    <td>${i + 1}</td>
                    <td>${s.name || s.rollNo}</td>
                    <td>${s.presentDays}</td>
                    <td>${s.absentDays}</td>
                    <td><span class="va-pct ${pctClass}">${pct}%</span></td>
                    <td><span class="${statusCls}">${statusTxt}</span></td>
                </tr>`;
            }
            tbl += '</tbody></table>';
            if (students.length > 15) tbl += `<br><em>Showing 15 of ${students.length}</em>`;
            addHtmlMessage(tbl);
        }

        goHome();
    }

    // ═══════════════════════════════════════
    // Smart Exam Renderer
    // ═══════════════════════════════════════
    function renderSmartExam(data) {
        const batch = data.batch;
        const filterLabel = data.filter ? '🔍 ' + capitalize(data.filter) + ' students' : '';

        let html = `<strong>📈 Exam Report — ${batch}</strong>`;
        if (filterLabel) html += `<br><span style="color: var(--va-accent); font-size: 0.75rem;">${filterLabel}</span>`;
        html += `<br>Total: <strong>${data.totalStudents}</strong><br><br>`;

        html += '<table class="va-data-table"><thead><tr>';
        html += '<th>#</th><th>Name</th><th>Exams</th><th>Marks</th><th>%</th>';
        html += '</tr></thead><tbody>';

        const students = data.students || [];
        const max = Math.min(students.length, 15);
        for (let i = 0; i < max; i++) {
            const s = students[i];
            const pct = s.percentage || 0;
            let cls = pct >= 75 ? 'va-pct-high' : pct >= 50 ? 'va-pct-mid' : 'va-pct-low';
            html += `<tr>
                <td>${i + 1}</td>
                <td>${s.name || s.rollNo}</td>
                <td>${s.attended || 0}/${s.totalExams || 0}</td>
                <td>${s.totalMarks || 0}</td>
                <td><span class="va-pct ${cls}">${pct}%</span></td>
            </tr>`;
        }
        html += '</tbody></table>';
        if (students.length > 15) html += `<br><em>Showing 15 of ${students.length}</em>`;

        addHtmlMessage(html);
        goHome();
    }

    // ═══════════════════════════════════════
    // Smart Mock Renderer
    // ═══════════════════════════════════════
    function renderSmartMock(data) {
        const batch = data.batch;
        const filterLabel = data.filter ? '🔍 ' + capitalize(data.filter) + ' students' : '';

        let html = `<strong>🎯 Mock Report — ${batch}</strong>`;
        if (filterLabel) html += `<br><span style="color: var(--va-accent); font-size: 0.75rem;">${filterLabel}</span>`;
        html += `<br>Total: <strong>${data.totalStudents}</strong><br><br>`;

        html += '<table class="va-data-table"><thead><tr>';
        html += '<th>#</th><th>Name</th><th>Mocks</th><th>Marks</th><th>%</th>';
        html += '</tr></thead><tbody>';

        const students = data.students || [];
        const max = Math.min(students.length, 15);
        for (let i = 0; i < max; i++) {
            const s = students[i];
            const pct = s.percentage || 0;
            let cls = pct >= 75 ? 'va-pct-high' : pct >= 50 ? 'va-pct-mid' : 'va-pct-low';
            html += `<tr>
                <td>${i + 1}</td>
                <td>${s.name || s.rollNo}</td>
                <td>${s.attended || 0}/${s.totalMocks || 0}</td>
                <td>${s.totalMarks || 0}</td>
                <td><span class="va-pct ${cls}">${pct}%</span></td>
            </tr>`;
        }
        html += '</tbody></table>';

        addHtmlMessage(html);
        goHome();
    }

    // ═══════════════════════════════════════
    // Smart Case-Study Renderer
    // ═══════════════════════════════════════
    function renderSmartCaseStudy(data) {
        const batch = data.batch;

        let html = `<strong>📋 Case-Study Report — ${batch}</strong>`;
        html += `<br>Total: <strong>${data.totalStudents}</strong><br><br>`;

        html += '<table class="va-data-table"><thead><tr>';
        html += '<th>#</th><th>Name</th><th>Attended</th>';
        html += '</tr></thead><tbody>';

        const students = data.students || [];
        const max = Math.min(students.length, 15);
        for (let i = 0; i < max; i++) {
            const s = students[i];
            html += `<tr>
                <td>${i + 1}</td>
                <td>${s.name || s.rollNo}</td>
                <td>${s.attended || 0}/${s.totalCaseStudies || 0}</td>
            </tr>`;
        }
        html += '</tbody></table>';

        addHtmlMessage(html);
        goHome();
    }

    // ============ FLOW STEP HANDLER ============
    function handleFlowStep(msg, original) {
        switch (session.step) {
            case 'waiting-date': handleDateInput(msg, original); break;
            case 'waiting-rolls': handleRollInput(msg, original); break;
            case 'waiting-roll-marks': handleRollMarksInput(msg, original); break;
            case 'waiting-confirm': handleConfirmation(msg); break;
            default: addMessage('bot', '🤔 Flow error. Returning to home.'); goHome();
        }
    }

    function handleDateInput(msg, original) {
        let dateStr = '';
        if (msg === 'today' || msg === 'now') {
            const t = new Date();
            dateStr = String(t.getDate()).padStart(2, '0') + '-' + String(t.getMonth() + 1).padStart(2, '0') + '-' + t.getFullYear();
        } else {
            const trimmed = original.trim();
            const m1 = trimmed.match(/^(\d{1,2})[\/\-](\d{1,2})[\/\-](\d{4})$/);
            if (m1) dateStr = m1[1].padStart(2, '0') + '-' + m1[2].padStart(2, '0') + '-' + m1[3];
            const m2 = trimmed.match(/^(\d{4})[\/\-](\d{1,2})[\/\-](\d{1,2})$/);
            if (!dateStr && m2) dateStr = m2[3].padStart(2, '0') + '-' + m2[2].padStart(2, '0') + '-' + m2[1];
            if (!dateStr) { addMessage('bot', '❌ Invalid date. Use **dd-mm-yyyy** or type **"today"**.'); return; }
        }
        session.data.date = dateStr;
        addMessage('bot', '✅ Date: **' + dateStr + '**');
        askForRollNumbers();
    }

    function handleRollInput(msg, original) {
        const cleaned = original.trim().replace(/[^0-9,\s\-]/g, '');
        if (!cleaned) { addMessage('bot', '❌ Enter valid roll numbers. Example: **1, 2, 3**'); return; }
        session.data.rollNos = cleaned;
        confirmAndSubmit();
    }

    function handleRollMarksInput(msg, original) {
        if (!original.trim().includes('=')) { addMessage('bot', '❌ Use format **roll=marks**. Example: **1=85, 2=90**'); return; }
        session.data.rollMarks = original.trim();
        confirmAndSubmit();
    }

    function handleConfirmation(msg) {
        if (['yes', 'y', 'confirm', 'ok', 'sure'].includes(msg)) {
            addMessage('bot', '⏳ Submitting...');
            executeSubmit();
        } else if (['no', 'n', 'cancel'].includes(msg)) {
            addMessage('bot', '❌ Cancelled.');
            goHome();
        } else {
            addMessage('bot', 'Type **"yes"** to confirm or **"no"** to cancel.');
        }
    }

    function containsAny(msg, ...words) {
        for (const w of words) { if (msg.includes(w)) return true; }
        return false;
    }

})();
