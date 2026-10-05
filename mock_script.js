const tracks = [
  { title: "Liquid Glass (Original Mix)", artist: "Audiophile Ensemble", hires: "FLAC 24/96", dur: "3:42", sec: 222 },
  { title: "Symphony in C Minor", artist: "Vienna Philharmonic", hires: "FLAC 24/192", dur: "7:15", sec: 435 },
  { title: "Acoustic Resonance", artist: "Neil Young (Master)", hires: "FLAC 24/96", dur: "4:18", sec: 258 },
  { title: "Midnight Velocity", artist: "Cyberwave Dreams", hires: "FLAC 16/44", dur: "5:02", sec: 302 },
  { title: "Echoes of Analog Glass", artist: "Glassworks Orchestra", hires: "FLAC 24/96", dur: "6:30", sec: 390 }
];

let currentIdx = 0, isPlaying = false, currentSec = 84, timer = null;

function switchTab(tab) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
  if (tab === 'library') {
    document.getElementById('tab-library').classList.add('active');
    document.getElementById('screen-library').classList.add('active');
  } else {
    document.getElementById('tab-settings').classList.add('active');
    document.getElementById('screen-settings').classList.add('active');
  }
}

function toggleSwitch(el) { el.classList.toggle('checked'); }

function selectTrack(idx) {
  currentIdx = idx; currentSec = 0; const t = tracks[idx];
  document.getElementById('mini-title').textContent = t.title;
  document.getElementById('mini-sub').textContent = t.artist + " · " + t.hires;
  document.getElementById('sheet-title').textContent = t.title;
  document.getElementById('sheet-artist').textContent = t.artist;
  document.getElementById('total-time').textContent = t.dur;
  document.querySelectorAll('.song-row').forEach((r, i) => r.classList.toggle('playing', i === idx));
  if (!isPlaying) togglePlay();
  updateProgress();
}

function togglePlay() {
  isPlaying = !isPlaying;
  const playSvg = '<path d="M6 4L18 12L6 20V4Z"/>';
  const pauseSvg = '<rect x="6" y="4" width="4" height="16"/><rect x="14" y="4" width="4" height="16"/>';
  document.getElementById('mini-play-icon').innerHTML = isPlaying ? pauseSvg : playSvg;
  document.getElementById('sheet-play-icon').innerHTML = isPlaying ? pauseSvg : playSvg;
  document.getElementById('sheet-art').classList.toggle('playing', isPlaying);
  if (isPlaying) {
    if (timer) clearInterval(timer);
    timer = setInterval(() => {
      currentSec++;
      if (currentSec > tracks[currentIdx].sec) nextTrack();
      else updateProgress();
    }, 1000);
  } else {
    if (timer) clearInterval(timer);
  }
}

function nextTrack() { selectTrack((currentIdx + 1) % tracks.length); }
function prevTrack() { selectTrack((currentIdx - 1 + tracks.length) % tracks.length); }

function updateProgress() {
  const t = tracks[currentIdx]; const pct = (currentSec / t.sec) * 100;
  document.getElementById('mini-progress').style.width = pct + "%";
  document.getElementById('seek-fill').style.width = pct + "%";
  document.getElementById('seek-thumb').style.left = pct + "%";
  const m = Math.floor(currentSec / 60); const s = String(currentSec % 60).padStart(2, '0');
  document.getElementById('elapsed-time').textContent = m + ":" + s;
}

function scrub(e) {
  const rect = e.currentTarget.getBoundingClientRect();
  const pct = Math.max(0, Math.min(1, (e.clientX - rect.left) / rect.width));
  currentSec = Math.floor(pct * tracks[currentIdx].sec);
  updateProgress();
}

function openNowPlaying() { document.getElementById('now-playing-sheet').classList.add('open'); }
function closeNowPlaying() { document.getElementById('now-playing-sheet').classList.remove('open'); }

function openUpdateDialog() {
  const modal = document.getElementById('update-modal'); modal.classList.add('active');
  document.getElementById('dialog-body').innerHTML = "Checking <strong>SamarVScode/vMusic</strong> releases on GitHub...<br><span style='color:var(--label-tertiary)'>Current version: v1.0.1 (Build 2)</span>";
  document.getElementById('dialog-primary-btn').textContent = "Check GitHub";
  document.getElementById('dialog-primary-btn').onclick = simulateCheck;
}

function closeUpdateDialog() { document.getElementById('update-modal').classList.remove('active'); }

function simulateCheck() {
  document.getElementById('dialog-body').innerHTML = "<div style='display:flex; align-items:center; gap:8px;'><span>Connecting to api.github.com...</span></div>";
  setTimeout(() => {
    document.getElementById('dialog-body').innerHTML = "<span style='color:#1ed760; font-weight:600;'>✓ You have the latest version!</span><br>vMusic v1.0.1 is currently installed with full Liquid Glass UI & Poweramp EQ engine.";
    document.getElementById('dialog-primary-btn').textContent = "Done";
    document.getElementById('dialog-primary-btn').onclick = closeUpdateDialog;
  }, 900);
}

function simulateScan() {
  const btn = document.querySelector('.scan-btn'); btn.textContent = "Scanning...";
  setTimeout(() => { btn.textContent = "5 Tracks Found"; setTimeout(() => btn.textContent = "Scan MediaStore", 1800); }, 700);
}

updateProgress();
