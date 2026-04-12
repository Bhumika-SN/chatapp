// ── CONSTANTS ──
const EMOJIS = ['😀','😂','😍','🥰','😎','🤔','😢','😡','👍','👎','❤️','🔥','🎉','💯','✅','🙏','😭','🤣','😊','😏','🫡','🤩','💀','😤','🥹','😴','🤓','👀','💬','🚀','⭐','💎','🎯','🏆','💡','🔐'];
const REACTS = ['❤️','😂','😮','😢','😡','👍'];

// ── STATE ──
let cu = null, ct = null, cr = null, cav = null;
let sc = null, typSub = null, seenSub = null, editSub = null, delSub = null, clearSub = null, snapSub = null, presSub = null;
let stompClient = null, typTO = null, allRooms = [], replyTo = null, isLogin = true;
let selectMode = false, selectedIds = new Set();
let mediaRec = null, audioChunks = [], isRecording = false;
let currentVaultTab = 'all', currentSnapId = null;

// ── OAUTH REDIRECT CHECK ──
(() => {
    const p = new URLSearchParams(location.search);
    if (p.get('token')) {
        ct = p.get('token');
        cu = p.get('username');
        cav = p.get('avatarUrl') || null;
        localStorage.setItem('ct', ct);
        localStorage.setItem('cu', cu);
        if (cav) localStorage.setItem('cav', cav);
        history.replaceState({}, '', '/');
        enterApp({ username: cu, avatarUrl: cav, avatarColor: '#E8622A' });
    }
})();

// ── AUTH ──
function swTab(m) {
    isLogin = m === 'login';
    document.getElementById('tl').classList.toggle('active', isLogin);
    document.getElementById('tr2').classList.toggle('active', !isLogin);
    document.getElementById('ab').textContent = isLogin ? 'Login' : 'Register';
}

async function doAuth() {
    const u = document.getElementById('au').value.trim();
    const p = document.getElementById('ap').value.trim();
    if (!u || !p) return showErr('Fill all fields');
    const res = await fetch(isLogin ? '/api/auth/login' : '/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: u, password: p })
    });
    const d = await res.json();
    if (d.error) return showErr(d.error);
    ct = d.token; cu = d.username;
    localStorage.setItem('ct', ct);
    localStorage.setItem('cu', cu);
    enterApp(d);
}

function showErr(m) {
    const e = document.getElementById('aerr');
    e.textContent = m;
    e.style.display = 'block';
}

function enterApp(d) {
    document.getElementById('auth-screen').style.display = 'none';
    document.getElementById('app').style.display = 'flex';
    const av = document.getElementById('my-av');
    if (d.avatarUrl) { av.innerHTML = `<img src="${d.avatarUrl}"/>`; cav = d.avatarUrl; }
    else { av.textContent = cu[0].toUpperCase(); av.style.background = d.avatarColor || '#E8622A'; }
    initEmoji(); connectWS(); loadRooms();
    applyTheme(localStorage.getItem('cth') || 'dark');
}

function logout() {
    if (stompClient) {
        stompClient.send('/app/online', {}, JSON.stringify({ username: cu, online: false }));
        stompClient.disconnect();
    }
    localStorage.clear();
    location.reload();
}

// ── WEBSOCKET ──
function connectWS() {
    stompClient = Stomp.over(new SockJS('/ws'));
    stompClient.debug = null;
    stompClient.connect({}, () => {
        stompClient.send('/app/online', {}, JSON.stringify({ username: cu, online: true }));
        presSub = stompClient.subscribe('/topic/presence', pl => {
            const d = JSON.parse(pl.body);
            if (cr && d.username !== cu)
                document.getElementById('ch-s').textContent = d.online ? `${d.username} is online` : '';
        });
    });
}

function H() { return { 'Content-Type': 'application/json', 'Authorization': 'Bearer ' + ct }; }

// ── ROOMS ──
async function loadRooms() {
    const r = await fetch('/api/rooms', { headers: H() });
    allRooms = await r.json();
    renderRooms(allRooms);
}

function renderRooms(rooms) {
    const list = document.getElementById('room-list');
    list.innerHTML = '';
    rooms.forEach(r => {
        const d = document.createElement('div');
        d.className = 'ri' + (cr === r.name ? ' active' : '');
        d.id = 'rm-' + r.name;
        const c = sCol(r.name);
        d.innerHTML = `<div class="r-av" style="background:${c}">${r.name[0].toUpperCase()}${r.locked ? '<span class="lock-badge">🔒</span>' : ''}</div>
          <div class="r-meta"><div class="r-nr"><span class="r-name">${r.name}</span><span class="r-time" id="rt-${r.name}"></span></div>
          <div class="r-prev" id="rp-${r.name}">No messages yet</div></div>`;
        d.onclick = () => tryOpenRoom(r.name, r.locked);
        list.appendChild(d);
    });
}

function filterRooms(q) {
    renderRooms(allRooms.filter(r => r.name.toLowerCase().includes(q.toLowerCase())));
}

async function createRoom() {
    const n = document.getElementById('nr-inp').value.trim();
    if (!n) return;
    await fetch('/api/rooms', { method: 'POST', headers: H(), body: JSON.stringify({ name: n, createdBy: cu }) });
    document.getElementById('nr-inp').value = '';
    await loadRooms();
    tryOpenRoom(n, false);
}

// ── LOCK / UNLOCK ──
async function tryOpenRoom(name, locked) {
    if (locked) {
        document.getElementById('unlock-modal').classList.add('open');
        document.getElementById('unlock-modal').dataset.room = name;
        document.getElementById('unlock-pw').value = '';
        setTimeout(() => document.getElementById('unlock-pw').focus(), 100);
        return;
    }
    openRoom(name);
}

async function doUnlock() {
    const modal = document.getElementById('unlock-modal');
    const room = modal.dataset.room;
    const pw = document.getElementById('unlock-pw').value;
    const r = await fetch(`/api/rooms/${room}/verify`, { method: 'POST', headers: H(), body: JSON.stringify({ password: pw }) });
    const d = await r.json();
    if (d.success) { modal.classList.remove('open'); openRoom(room); }
    else {
        document.getElementById('unlock-pw').style.borderColor = '#ff7055';
        setTimeout(() => document.getElementById('unlock-pw').style.borderColor = '', 1500);
    }
}
function closeUnlockModal() { document.getElementById('unlock-modal').classList.remove('open'); }

function openLockModal() {
    document.getElementById('lock-pw').value = '';
    document.getElementById('lock-modal').classList.add('open');
    setTimeout(() => document.getElementById('lock-pw').focus(), 100);
}
function closeLockModal() { document.getElementById('lock-modal').classList.remove('open'); }
async function saveLock() {
    const pw = document.getElementById('lock-pw').value;
    await fetch(`/api/rooms/${cr}/lock`, { method: 'POST', headers: H(), body: JSON.stringify({ password: pw }) });
    closeLockModal();
    await loadRooms();
}

// ── OPEN ROOM ──
async function openRoom(roomName) {
    cr = roomName; replyTo = null; selectMode = false; selectedIds.clear();
    document.getElementById('reply-bar').style.display = 'none';
    document.getElementById('sel-bar').style.display = 'none';
    document.getElementById('no-room').style.display = 'none';
    document.getElementById('ch-header').style.display = 'flex';
    document.getElementById('msgs').style.display = 'flex';
    document.getElementById('msgs').style.flexDirection = 'column';
    document.getElementById('inp-area').style.display = 'flex';

    const c = sCol(roomName);
    document.getElementById('ch-av').textContent = roomName[0].toUpperCase();
    document.getElementById('ch-av').style.background = c;
    document.getElementById('ch-t').textContent = roomName;

    document.querySelectorAll('.ri').forEach(el => el.classList.remove('active'));
    document.getElementById('rm-' + roomName)?.classList.add('active');

    [sc, typSub, seenSub, editSub, delSub, clearSub, snapSub].forEach(s => s?.unsubscribe());

    sc = stompClient.subscribe(`/topic/room/${roomName}`, pl => {
        const d = JSON.parse(pl.body);
        if (d.type === 'reaction') updateReacts(d.message);
        else { appendMsg(d); updPreview(roomName, d); }
    });
    typSub = stompClient.subscribe(`/topic/typing/${roomName}`, pl => {
        const d = JSON.parse(pl.body);
        if (d.username !== cu) document.getElementById('typ').textContent = d.typing ? `${d.username} is typing...` : '';
    });
    seenSub = stompClient.subscribe(`/topic/seen/${roomName}`, pl => updSeenTick(JSON.parse(pl.body)));
    editSub = stompClient.subscribe(`/topic/edit/${roomName}`, pl => updMsgContent(JSON.parse(pl.body)));
    delSub  = stompClient.subscribe(`/topic/delete/${roomName}`, pl => updMsgContent(JSON.parse(pl.body)));
    clearSub = stompClient.subscribe(`/topic/clear/${roomName}`, () => { document.getElementById('msgs').innerHTML = ''; });
    snapSub = stompClient.subscribe(`/topic/snap/${roomName}`, pl => {
        const d = JSON.parse(pl.body);
        const el = document.getElementById('msg-' + d.id);
        if (el) { const ov = el.querySelector('.snap-overlay'); if (ov) ov.textContent = '👁️ Viewed'; }
    });

    document.getElementById('msgs').innerHTML = '';
    const res = await fetch(`/api/messages/${roomName}`, { headers: H() });
    const msgs = await res.json();
    msgs.forEach(m => appendMsg(m));
    scrollBot();
}

// ── SEND MESSAGE ──
function sendMsg() {
    const inp = document.getElementById('mi');
    const content = inp.value.trim();
    if (!content || !cr || !stompClient) return;
    const snapCheck = document.getElementById('snap-check');
    const isSnap = snapCheck && snapCheck.checked;
    stompClient.send('/app/sendMessage', {}, JSON.stringify({
        sender: cu, content, roomName: cr,
        replyToId: replyTo?.id?.toString(), snapView: isSnap
    }));
    inp.value = ''; inp.style.height = 'auto';
    cancelReply(); stopTyping();
}

// ── FILE UPLOAD ──
async function uploadFile(input) {
    const file = input.files[0]; if (!file) return;
    const fd = new FormData(); fd.append('file', file);
    const res = await fetch('/api/files/upload', { method: 'POST', headers: { 'Authorization': 'Bearer ' + ct }, body: fd });
    const d = await res.json();
    if (d.success) {
        stompClient.send('/app/sendMessage', {}, JSON.stringify({
            sender: cu, content: d.name, roomName: cr, fileUrl: d.url, fileType: d.type, fileName: d.name
        }));
    }
    input.value = '';
}

// ── VOICE RECORDING ──
async function toggleRecording() {
    const btn = document.getElementById('rec-btn');
    if (!isRecording) {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
            mediaRec = new MediaRecorder(stream); audioChunks = [];
            mediaRec.ondataavailable = e => audioChunks.push(e.data);
            mediaRec.onstop = async () => {
                const blob = new Blob(audioChunks, { type: 'audio/webm' });
                const fd = new FormData(); fd.append('file', blob, 'voice.webm');
                const res = await fetch('/api/files/upload', { method: 'POST', headers: { 'Authorization': 'Bearer ' + ct }, body: fd });
                const d = await res.json();
                if (d.success) stompClient.send('/app/sendMessage', {}, JSON.stringify({
                    sender: cu, content: '🎤 Voice message', roomName: cr, fileUrl: d.url, fileType: 'audio', fileName: 'voice.webm'
                }));
                stream.getTracks().forEach(t => t.stop());
            };
            mediaRec.start(); isRecording = true;
            btn.classList.add('recording'); btn.title = 'Stop recording';
        } catch (e) { alert('Microphone access denied'); }
    } else {
        mediaRec.stop(); isRecording = false;
        btn.classList.remove('recording'); btn.title = 'Voice message';
    }
}

// ── RENDER MESSAGE ──
function appendMsg(msg) {
    const wrap = document.getElementById('msgs');
    if (document.getElementById('msg-' + msg.id)) { updMsgContent(msg); return; }
    const isMine = msg.sender === cu;
    const div = document.createElement('div');
    div.id = 'msg-' + msg.id; div.className = 'msg-row';

    let inner = '';
    if (msg.replyToId) inner += `<div class="rp">↩ Replying to a message</div>`;
    if (!isMine) inner += `<div class="msn">${msg.sender}</div>`;

    if (msg.deleted) {
        inner += `<div class="mdel">🚫 This message was deleted</div>`;
    } else if (msg.fileType === 'image' && msg.fileUrl) {
        if (msg.snapView && !msg.snapViewed) {
            inner += `<div class="snap-bubble"><img class="img-bubble snap-img" src="${msg.fileUrl}" style="filter:blur(20px);pointer-events:none"/><div class="snap-overlay" onclick="viewSnap(${msg.id},'${msg.fileUrl}')">👁️ Tap to view once</div></div>`;
        } else if (msg.snapView && msg.snapViewed) {
            inner += `<div style="padding:8px;color:var(--ts);font-style:italic;font-size:13px;">👁️ Snap already viewed</div>`;
        } else {
            inner += `<img class="img-bubble" src="${msg.fileUrl}" onclick="window.open('${msg.fileUrl}')"/>`;
        }
    } else if (msg.fileType === 'audio' && msg.fileUrl) {
        inner += `<audio class="audio-bubble" controls src="${msg.fileUrl}"></audio>`;
    } else if (msg.fileUrl) {
        inner += `<div class="file-bubble" onclick="window.open('${msg.fileUrl}')"><svg viewBox="0 0 24 24"><path d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z"/></svg><span style="font-size:13px;color:var(--tp)">${msg.fileName || msg.content}</span></div>`;
    } else {
        inner += `<span class="mc">${escH(msg.content)}</span>`;
    }

    const seen = msg.seenBy ? msg.seenBy.split(',').filter(Boolean).length : 0;
    const tickHtml = isMine ? `<span class="mtk ${seen > 0 ? 'tks' : ''}">${seen > 0 ? '✓✓' : '✓'}</span>` : '';
    const editHtml = msg.edited && !msg.deleted ? `<span class="med">(edited)</span>` : '';
    inner += `<div class="mft">${editHtml}<span class="mt">${msg.formattedTime || ''}</span>${tickHtml}</div>`;
    inner += `<div class="mrc" id="rc-${msg.id}">${buildReacts(msg)}</div>`;

    if (!msg.deleted) {
        inner += `<div class="ma">
          <div class="mab" onclick="setReply(${msg.id},'${escJ(msg.sender)}','${escJ(msg.content)}')">↩ Reply</div>
          ${REACTS.map(e => `<div class="mab" onclick="sendReact(${msg.id},'${e}')">${e}</div>`).join('')}
          ${isMine ? `<div class="mab" onclick="startEdit(${msg.id},'${escJ(msg.content)}')">✏️ Edit</div>` : ''}
          ${isMine ? `<div class="mab danger" onclick="delMsg(${msg.id})">🗑️ Delete</div>` : ''}
          <div class="mab" onclick="starMsg(${msg.id})">⭐ Star</div>
        </div>`;
    }

    const starC = msg.starred ? ' starred' : '';
    div.innerHTML = `<div class="mb ${isMine ? 'mine' : 'other'}${starC}" style="position:relative;" onclick="handleMsgClick(${msg.id},event)">${inner}</div>`;
    wrap.appendChild(div);
    if (!isMine && msg.id) setTimeout(() => stompClient?.send('/app/seen', {}, JSON.stringify({ messageId: msg.id, username: cu })), 800);
    scrollBot();
    updPreview(cr, msg);
}

// ── SELECT MODE ──
function handleMsgClick(id, e) {
    if (!selectMode || e.target.closest('.ma')) return;
    const el = document.getElementById('msg-' + id); if (!el) return;
    const bubble = el.querySelector('.mb');
    if (selectedIds.has(id)) { selectedIds.delete(id); bubble.classList.remove('selected'); }
    else { selectedIds.add(id); bubble.classList.add('selected'); }
    document.getElementById('sel-count').textContent = selectedIds.size + ' selected';
}

function toggleSelect() {
    selectMode = !selectMode; selectedIds.clear();
    document.getElementById('sel-bar').style.display = selectMode ? 'flex' : 'none';
    document.querySelectorAll('.mb').forEach(b => { b.classList.remove('selected'); b.style.cursor = selectMode ? 'pointer' : ''; });
    if (!selectMode) document.getElementById('sel-count').textContent = '0 selected';
}

async function deleteSelected() {
    if (!selectedIds.size) return;
    await fetch('/api/messages/delete-selected', { method: 'POST', headers: H(), body: JSON.stringify({ ids: [...selectedIds], username: cu }) });
    selectedIds.forEach(id => {
        const el = document.getElementById('msg-' + id);
        if (el) { const b = el.querySelector('.mb'); if (b) b.innerHTML = `<div class="mdel">🚫 This message was deleted</div>`; }
    });
    selectedIds.clear(); toggleSelect();
}

async function clearChat() {
    if (!confirm('Clear entire chat? This cannot be undone.')) return;
    await fetch(`/api/rooms/${cr}/clear?username=${cu}`, { method: 'DELETE', headers: H() });
    document.getElementById('msgs').innerHTML = '';
    toggleSelect();
}

// ── SNAP VIEW ──
function viewSnap(id, url) {
    currentSnapId = id;
    document.getElementById('snap-img').src = url;
    document.getElementById('snap-view').classList.add('open');
}
async function closeSnap() {
    document.getElementById('snap-view').classList.remove('open');
    document.getElementById('snap-img').src = '';
    if (currentSnapId) {
        await fetch(`/api/messages/${currentSnapId}/snap-viewed`, { method: 'PUT', headers: H() });
        const el = document.getElementById('msg-' + currentSnapId);
        if (el) { const ov = el.querySelector('.snap-overlay'); if (ov) ov.textContent = '👁️ Viewed'; }
        currentSnapId = null;
    }
}

// ── REACTIONS ──
function buildReacts(msg) {
    if (!msg.reactions || !msg.reactions.length) return '';
    const c = {};
    msg.reactions.forEach(r => { const [, e] = r.split(':'); c[e] = (c[e] || 0) + 1; });
    return Object.entries(c).map(([e, n]) => `<span class="rchip" onclick="sendReact(${msg.id},'${e}')">${e} ${n}</span>`).join('');
}

function updMsgContent(msg) {
    const el = document.getElementById('msg-' + msg.id); if (!el) return;
    const b = el.querySelector('.mb'); if (!b) return;
    if (msg.deleted) {
        const ft = b.querySelector('.mft');
        b.innerHTML = `<div class="mdel">🚫 This message was deleted</div>${ft ? ft.outerHTML : ''}`;
        return;
    }
    const mc = b.querySelector('.mc'); if (mc) mc.textContent = msg.content;
    const re = document.getElementById('rc-' + msg.id); if (re) re.innerHTML = buildReacts(msg);
    b.classList.toggle('starred', !!msg.starred);
}

function updateReacts(msg) { const re = document.getElementById('rc-' + msg.id); if (re) re.innerHTML = buildReacts(msg); }
function updSeenTick(msg) { const el = document.getElementById('msg-' + msg.id); if (!el) return; const t = el.querySelector('.mtk'); if (t) { t.textContent = '✓✓'; t.classList.add('tks'); } }
function sendReact(id, e) { stompClient.send('/app/react', {}, JSON.stringify({ messageId: id.toString(), username: cu, emoji: e })); }

// ── REPLY / EDIT ──
function setReply(id, s, c) {
    replyTo = { id, sender: s, content: c };
    document.getElementById('reply-bar').style.display = 'flex';
    document.getElementById('ri2').textContent = `↩ ${s}: ${c.substring(0, 50)}`;
    document.getElementById('mi').focus();
}
function cancelReply() { replyTo = null; document.getElementById('reply-bar').style.display = 'none'; }

let editingId = null;
function startEdit(id, c) { editingId = id; const i = document.getElementById('mi'); i.value = c; i.focus(); i.style.borderColor = 'var(--or)'; }

async function delMsg(id) {
    const r = await fetch(`/api/messages/${id}/delete`, { method: 'PUT', headers: H(), body: JSON.stringify({ username: cu }) });
    const d = await r.json(); if (d.success) updMsgContent(d.message);
}

async function starMsg(id) {
    const r = await fetch(`/api/messages/${id}/star`, { method: 'PUT', headers: H(), body: JSON.stringify({ username: cu }) });
    const d = await r.json();
    if (d.success) { const el = document.getElementById('msg-' + id); if (el) el.querySelector('.mb')?.classList.toggle('starred', d.starred); }
}

// ── TYPING ──
function handleTyping() {
    const inp = document.getElementById('mi');
    if (editingId) {
        clearTimeout(typTO);
        typTO = setTimeout(async () => {
            const c = inp.value.trim(); if (!c) { editingId = null; inp.style.borderColor = ''; return; }
            const id = editingId; editingId = null; inp.style.borderColor = '';
            const r = await fetch(`/api/messages/${id}/edit`, { method: 'PUT', headers: H(), body: JSON.stringify({ content: c, username: cu }) });
            const d = await r.json(); if (d.success) updMsgContent(d.message);
            inp.value = ''; inp.style.height = 'auto';
        }, 900);
        return;
    }
    if (!stompClient || !cr) return;
    stompClient.send('/app/typing', {}, JSON.stringify({ username: cu, roomName: cr, typing: true }));
    clearTimeout(typTO); typTO = setTimeout(stopTyping, 2000);
}
function stopTyping() { if (stompClient && cr) stompClient.send('/app/typing', {}, JSON.stringify({ username: cu, roomName: cr, typing: false })); }

// ── SEARCH ──
function toggleSearch() {
    const b = document.getElementById('smsg-bar');
    b.style.display = b.style.display === 'flex' ? 'none' : 'flex';
    if (b.style.display === 'flex') b.querySelector('input').focus();
}

async function searchMsgs(q) {
    if (!cr || q.length < 2) return;
    const r = await fetch(`/api/messages/${cr}/search?q=${encodeURIComponent(q)}`, { headers: H() });
    const msgs = await r.json();
    document.getElementById('msgs').innerHTML = '';
    msgs.forEach(m => appendMsg(m));
}

// ── AI SUMMARY ──
async function getAISummary() {
    document.getElementById('ai-modal').classList.add('open');
    document.getElementById('ai-text').textContent = 'Generating...';
    const r = await fetch(`/api/messages/${cr}`, { headers: H() });
    const msgs = await r.json();
    if (!msgs.length) { document.getElementById('ai-text').textContent = 'No messages to summarize.'; return; }
    const txt = msgs.slice(-20).filter(m => !m.deleted && !m.fileUrl).map(m => `${m.sender}: ${m.content}`).join('\n');
    try {
        const ar = await fetch('/api/ai/summarize', { method: 'POST', headers: H(), body: JSON.stringify({ messages: txt }) });
        const d = await ar.json();
        document.getElementById('ai-text').textContent = d.error ? '❌ ' + d.error : d.summary;
    } catch (e) { document.getElementById('ai-text').textContent = 'AI service unavailable.'; }
}

function updPreview(r, msg) {
    const p = document.getElementById('rp-' + r), t = document.getElementById('rt-' + r);
    if (p) p.textContent = msg.deleted ? 'Message deleted' : msg.fileType ? `📎 ${msg.fileName || 'File'}` : msg.content ? `${msg.sender}: ${msg.content}` : '';
    if (t) t.textContent = msg.formattedTime || '';
}

// ── VAULT ──
function switchNav(tab) {
    document.getElementById('nav-chat').classList.toggle('active', tab === 'chat');
    document.getElementById('nav-vault').classList.toggle('active', tab === 'vault');
    document.getElementById('chat-area').style.display = tab === 'chat' ? 'flex' : 'none';
    const v = document.getElementById('vault');
    v.classList.toggle('open', tab === 'vault');
    if (tab === 'vault') loadVault();
}

async function loadVault() {
    const items = await (await fetch(`/api/vault/${cu}`, { headers: H() })).json();
    renderVaultItems(items);
    renderVaultForm(currentVaultTab);
}

function switchVaultTab(tab) {
    currentVaultTab = tab;
    document.querySelectorAll('.vt-tab').forEach(t => t.classList.remove('active'));
    document.getElementById('vtt-' + tab).classList.add('active');
    loadVault();
}

function renderVaultForm(tab) {
    const f = document.getElementById('vt-add-form');
    if (tab === 'note' || tab === 'all') {
        f.innerHTML = `<div class="vt-add">
          <input id="vf-title" placeholder="Note title..."/>
          <textarea id="vf-content" rows="2" placeholder="Note content..." style="resize:none;"></textarea>
          <button onclick="addVaultNote()">+ Note</button>
        </div>`;
    } else if (tab === 'schedule') {
        f.innerHTML = `<div class="vt-add" style="flex-wrap:wrap;gap:6px;">
          <input id="vf-title" placeholder="Message to schedule..." style="flex:1;min-width:200px;"/>
          <input id="vf-room" placeholder="Room name" style="max-width:140px;"/>
          <input id="vf-time" type="datetime-local" style="max-width:180px;"/>
          <button onclick="addVaultSchedule()">+ Schedule</button>
        </div>`;
    } else if (tab === 'timer') {
        f.innerHTML = `<div class="vt-add">
          <input id="vf-title" placeholder="Task title..."/>
          <input id="vf-content" placeholder="Description (optional)"/>
          <button onclick="addVaultTimer()">+ Task</button>
        </div>`;
    } else if (tab === 'file') {
        f.innerHTML = `<div class="vt-add">
          <input type="file" id="vf-file" style="flex:1;padding:8px;border:1.5px solid var(--bd);border-radius:9px;background:var(--bg3);color:var(--tp);"/>
          <button onclick="uploadVaultFile()">+ Upload</button>
        </div>`;
    } else {
        f.innerHTML = '';
    }
}

function renderVaultItems(items) {
    const c = document.getElementById('vt-items'); c.innerHTML = '';
    const filtered = currentVaultTab === 'all' ? items : items.filter(i => i.type === currentVaultTab);
    if (!filtered.length) { c.innerHTML = `<div style="text-align:center;color:var(--ts);padding:30px;font-size:14px;">Nothing here yet</div>`; return; }
    filtered.forEach(item => {
        const d = document.createElement('div');
        d.className = 'vcard' + (item.completed ? ' done' : '');
        const icons = { note: '📝', file: '📁', schedule: '⏰', timer: '⚡' };
        let sub = '';
        if (item.type === 'note') sub = item.content || '';
        else if (item.type === 'file') sub = `<a href="${item.fileUrl}" target="_blank" style="color:var(--or)">Open file</a>`;
        else if (item.type === 'schedule') sub = `Room: ${item.scheduledRoom || '—'} | ${item.scheduledAt ? new Date(item.scheduledAt).toLocaleString() : 'No time set'}`;
        else if (item.type === 'timer') sub = item.content || '';
        d.innerHTML = `<div class="vc-icon">${icons[item.type] || '📄'}</div>
          <div class="vc-body">
            <div class="vc-title">${escH(item.title || 'Untitled')}</div>
            <div class="vc-sub">${sub}</div>
            <div class="vc-acts">
              ${item.type === 'timer' ? `<button class="vc-btn" onclick="toggleTask(${item.id})">${item.completed ? '↩ Undo' : '✓ Done'}</button>` : ''}
              ${item.type === 'schedule' && !item.sent ? `<button class="vc-btn" onclick="sendScheduledNow(${item.id},'${escJ(item.title)}','${escJ(item.scheduledRoom)}')">▶ Send now</button>` : ''}
            </div>
          </div>
          <button class="vc-del" onclick="delVaultItem(${item.id})">🗑️</button>`;
        c.appendChild(d);
    });
}

async function addVaultNote() {
    const t = document.getElementById('vf-title')?.value.trim();
    const c = document.getElementById('vf-content')?.value.trim();
    if (!t) return;
    await fetch('/api/vault/note', { method: 'POST', headers: H(), body: JSON.stringify({ owner: cu, title: t, content: c }) });
    loadVault();
}
async function addVaultSchedule() {
    const t = document.getElementById('vf-title')?.value.trim();
    const room = document.getElementById('vf-room')?.value.trim();
    const time = document.getElementById('vf-time')?.value;
    if (!t) return;
    await fetch('/api/vault/schedule', { method: 'POST', headers: H(), body: JSON.stringify({ owner: cu, title: t, room, scheduledAt: time ? new Date(time).toISOString().slice(0, 19) : null }) });
    loadVault();
}
async function addVaultTimer() {
    const t = document.getElementById('vf-title')?.value.trim();
    const c = document.getElementById('vf-content')?.value.trim();
    if (!t) return;
    await fetch('/api/vault/timer', { method: 'POST', headers: H(), body: JSON.stringify({ owner: cu, title: t, content: c }) });
    loadVault();
}
async function uploadVaultFile() {
    const fi = document.getElementById('vf-file'); if (!fi.files[0]) return;
    const fd = new FormData(); fd.append('file', fi.files[0]); fd.append('owner', cu);
    await fetch('/api/vault/file', { method: 'POST', headers: { 'Authorization': 'Bearer ' + ct }, body: fd });
    loadVault();
}
async function toggleTask(id) {
    await fetch(`/api/vault/${id}/complete`, { method: 'PUT', headers: H(), body: JSON.stringify({ owner: cu }) });
    loadVault();
}
async function delVaultItem(id) {
    await fetch(`/api/vault/${id}?owner=${cu}`, { method: 'DELETE', headers: H() });
    loadVault();
}
async function sendScheduledNow(id, content, room) {
    if (!room) return alert('No room set for this scheduled message');
    stompClient.send('/app/sendMessage', {}, JSON.stringify({ sender: cu, content, roomName: room }));
    await fetch(`/api/vault/${id}?owner=${cu}`, { method: 'DELETE', headers: H() });
    loadVault();
}

// ── PROFILE ──
function openProfile() {
    document.getElementById('pp-nm').textContent = cu;
    document.getElementById('pp-un').textContent = '@' + cu;
    const a = document.getElementById('pp-av');
    if (cav) a.innerHTML = `<img src="${cav}"/>`;
    else { a.textContent = cu[0].toUpperCase(); a.style.background = 'var(--or)'; }
    document.getElementById('pp').classList.add('open');
}
async function saveProfile() {
    const s = document.getElementById('pp-st').value;
    await fetch('/api/auth/profile', { method: 'PUT', headers: H(), body: JSON.stringify({ username: cu, statusMessage: s }) });
    document.getElementById('pp').classList.remove('open');
}

// ── EMOJI ──
function initEmoji() {
    const g = document.getElementById('egrid');
    EMOJIS.forEach(e => {
        const b = document.createElement('span');
        b.className = 'eb'; b.textContent = e;
        b.onclick = () => { document.getElementById('mi').value += e; document.getElementById('ep').classList.remove('open'); };
        g.appendChild(b);
    });
}
function toggleEmoji() { document.getElementById('ep').classList.toggle('open'); }
document.addEventListener('click', e => { if (!e.target.closest('.ew')) document.getElementById('ep').classList.remove('open'); });

// ── THEME ──
function applyTheme(t) { document.documentElement.setAttribute('data-theme', t); localStorage.setItem('cth', t); }
function toggleTheme() { applyTheme(document.documentElement.getAttribute('data-theme') === 'dark' ? 'light' : 'dark'); }

// ── UTILS ──
function autoResize(el) { el.style.height = 'auto'; el.style.height = Math.min(el.scrollHeight, 110) + 'px'; }
function scrollBot() { const w = document.getElementById('msgs'); w.scrollTop = w.scrollHeight; }
function sCol(s) {
    const c = ['#E8622A','#D4A853','#6B8E6B','#5B8DB8','#9B6B9B','#B87355','#7A8B7A','#C47A55'];
    let h = 0; for (let i = 0; i < s.length; i++) h = s.charCodeAt(i) + ((h << 5) - h);
    return c[Math.abs(h) % c.length];
}
function escH(s) { return String(s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }
function escJ(s) { return String(s || '').replace(/'/g, "\\'").replace(/\n/g, '\\n'); }

// ── AUTO LOGIN ──
const st = localStorage.getItem('ct'), su = localStorage.getItem('cu'), sa = localStorage.getItem('cav');
if (st && su) {
    ct = st; cu = su; cav = sa || null;
    fetch('/api/rooms', { headers: H() }).then(r => { if (r.ok) enterApp({ username: su, avatarUrl: cav, avatarColor: '#E8622A' }); });
}
applyTheme(localStorage.getItem('cth') || 'dark');
