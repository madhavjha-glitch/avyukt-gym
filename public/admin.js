import { initializeApp } from 'https://www.gstatic.com/firebasejs/11.0.2/firebase-app.js';
import {
  getAuth,
  getRedirectResult,
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signInWithRedirect,
  signOut
} from 'https://www.gstatic.com/firebasejs/11.0.2/firebase-auth.js';
import {
  collection,
  deleteDoc,
  doc,
  getFirestore,
  onSnapshot,
  orderBy,
  query,
  runTransaction,
  serverTimestamp,
  setDoc,
  updateDoc
} from 'https://www.gstatic.com/firebasejs/11.0.2/firebase-firestore.js';

const firebaseConfig = {
  apiKey: 'AIzaSyClpe-ccQIphDSknUjKIG4fawsyxfPBKLs',
  authDomain: 'avyukt-gym.firebaseapp.com',
  projectId: 'avyukt-gym',
  storageBucket: 'avyukt-gym.firebasestorage.app',
  messagingSenderId: '531967221920',
  appId: '1:531967221920:web:3c9606cee69d3a9dc9a9c5'
};

const FEES_AMOUNT = 500;
const OWNER_PHONE_RAW = '919990715771';

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getFirestore(app);
const googleProvider = new GoogleAuthProvider();

const authGate = document.getElementById('auth-gate');
const adminShell = document.getElementById('admin-shell');
const signInBtn = document.getElementById('google-signin-btn');
const signInLabel = document.getElementById('google-signin-label');
const authError = document.getElementById('auth-error');
const signOutBtn = document.getElementById('signout-btn');
const userEmailBadge = document.getElementById('user-email-badge');
const syncStatusEl = document.getElementById('sync-status');
const toastEl = document.getElementById('toast');

let currentUid = null;
let unsubs = [];
let members = [];
let allAttendanceDays = {};
let allFeeMonths = {};
let selectedAttDate = todayIso();
let selectedFeesMonth = currentMonthIso();
let memberBatchFilter = 'ALL';
let attBatchFilter = 'ALL';
let feeFilter = 'ALL';
let editingMemberId = null;

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function currentMonthIso() {
  return todayIso().slice(0, 7);
}

function formatMonthLabel(ym) {
  const [y, m] = ym.split('-').map(Number);
  if (!y || !m) return ym;
  return new Date(y, m - 1, 1).toLocaleDateString('en-US', { month: 'short', year: 'numeric' });
}

function formatDisplayDate(iso) {
  if (!iso || !/^\d{4}-\d{2}-\d{2}$/.test(iso)) return iso || '';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}

function batchLabel(batch) {
  if (batch === 'EVENING') return 'Evening';
  if (batch === 'BOTH') return 'Both';
  return 'Morning';
}

function matchesBatch(batch, filter) {
  if (filter === 'ALL') return true;
  if (batch === 'BOTH') return true;
  return (batch || 'MORNING') === filter;
}

function showToast(msg) {
  toastEl.textContent = msg;
  toastEl.classList.remove('hidden');
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => toastEl.classList.add('hidden'), 2600);
}

function computeDueInfo(joinDate, isPaid) {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const joinDay = Number((joinDate || '').split('-')[2]) || 1;
  const daysInMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).getDate();
  const dueDate = new Date(today.getFullYear(), today.getMonth(), Math.min(joinDay, daysInMonth));
  const diffDays = Math.round((dueDate - today) / 86400000);
  if (isPaid) return { urgency: 'PAID', daysRemaining: diffDays, dueDate };
  if (diffDays < 0) return { urgency: 'OVERDUE', daysRemaining: diffDays, dueDate };
  if (diffDays <= 5) return { urgency: 'DUE_SOON', daysRemaining: diffDays, dueDate };
  return { urgency: 'UPCOMING', daysRemaining: diffDays, dueDate };
}

function updateSyncBadge() {
  if (!navigator.onLine) {
    syncStatusEl.textContent = 'Offline Mode';
    syncStatusEl.className = 'sync-status offline';
  } else {
    syncStatusEl.textContent = 'Cloud Connected';
    syncStatusEl.className = 'sync-status online';
  }
}

window.addEventListener('online', updateSyncBadge);
window.addEventListener('offline', updateSyncBadge);
updateSyncBadge();

if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {});
  });
}

function formatAuthError(err) {
  const code = err?.code || '';
  if (code === 'auth/unauthorized-domain') {
    return `Domain "${window.location.hostname}" Firebase Authorized Domains me add nahi hai. Firebase Console (avyukt-gym) → Authentication → Settings → Authorized domains me "${window.location.hostname}" add karein.`;
  }
  if (code === 'auth/operation-not-allowed') {
    return 'Firebase Console (avyukt-gym) → Authentication → Sign-in method me Google provider enable karein.';
  }
  if (code === 'auth/popup-closed-by-user') {
    return 'Sign-in cancel ho gaya. Dobara try karein.';
  }
  return err?.message || 'Google Sign-In fail ho gaya.';
}

getRedirectResult(auth).catch((err) => {
  if (err) {
    authError.textContent = formatAuthError(err);
    authError.classList.remove('hidden');
  }
});

signInBtn.addEventListener('click', async () => {
  authError.classList.add('hidden');
  signInBtn.disabled = true;
  signInLabel.textContent = 'Signing in...';
  try {
    await signInWithPopup(auth, googleProvider);
  } catch (err) {
    const code = err?.code || '';
    if (code === 'auth/popup-blocked' || code === 'auth/operation-not-supported-in-this-environment') {
      try {
        await signInWithRedirect(auth, googleProvider);
        return;
      } catch (redirectErr) {
        authError.textContent = formatAuthError(redirectErr);
      }
    } else {
      authError.textContent = formatAuthError(err);
    }
    authError.classList.remove('hidden');
  } finally {
    signInBtn.disabled = false;
    signInLabel.textContent = 'Sign in with Google';
  }
});

signOutBtn.addEventListener('click', async () => {
  await signOut(auth);
});

onAuthStateChanged(auth, (user) => {
  unsubs.forEach((u) => u());
  unsubs = [];

  if (!user) {
    currentUid = null;
    authGate.classList.remove('hidden');
    adminShell.classList.add('hidden');
    return;
  }

  currentUid = user.uid;
  userEmailBadge.textContent = user.email || '';
  authGate.classList.add('hidden');
  adminShell.classList.remove('hidden');

  const membersRef = query(collection(db, 'users', currentUid, 'members'), orderBy('createdAt', 'asc'));
  const attRef = collection(db, 'users', currentUid, 'attendance');
  const feesRef = collection(db, 'users', currentUid, 'fees');

  unsubs.push(
    onSnapshot(membersRef, (snap) => {
      members = snap.docs.map((d) => ({ id: d.id, ...d.data() }));
      renderAll();
    }, (err) => showToast(err.message || 'Members load error'))
  );

  unsubs.push(
    onSnapshot(attRef, (snap) => {
      const next = {};
      snap.docs.forEach((d) => {
        next[d.id] = d.data()?.records || {};
      });
      allAttendanceDays = next;
      renderAll();
    })
  );

  unsubs.push(
    onSnapshot(feesRef, (snap) => {
      const next = {};
      snap.docs.forEach((d) => {
        next[d.id] = d.data()?.records || {};
      });
      allFeeMonths = next;
      renderAll();
    })
  );
});

function switchTab(tabName) {
  document.querySelectorAll('.tab-panel').forEach((p) => p.classList.remove('active'));
  document.getElementById(`tab-${tabName}`)?.classList.add('active');
  document.querySelectorAll('.nav-item').forEach((n) => {
    n.classList.toggle('active', n.dataset.tab === tabName);
  });
}

document.querySelectorAll('.nav-item').forEach((btn) => {
  btn.addEventListener('click', () => switchTab(btn.dataset.tab));
});

document.querySelectorAll('[data-nav]').forEach((btn) => {
  btn.addEventListener('click', () => {
    if (btn.dataset.filter) {
      feeFilter = btn.dataset.filter;
      selectedFeesMonth = currentMonthIso();
    }
    switchTab(btn.dataset.nav);
    renderAll();
  });
});

// Members modal
const memberModal = document.getElementById('member-modal');
const memberForm = document.getElementById('member-form');
document.getElementById('open-add-member-btn').addEventListener('click', () => openMemberModal(null));
document.getElementById('member-modal-cancel').addEventListener('click', () => memberModal.classList.add('hidden'));

function openMemberModal(member) {
  editingMemberId = member ? member.id : null;
  document.getElementById('member-modal-title').textContent = member ? 'Member Edit Karo' : 'Naya Member';
  document.getElementById('form-name').value = member ? member.name : '';
  document.getElementById('form-phone').value = member ? member.phone : '';
  document.getElementById('form-batch').value = member?.batch || 'MORNING';
  document.getElementById('form-join-date').value = member?.joinDate || todayIso();
  memberModal.classList.remove('hidden');
}

memberForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  if (!currentUid) return;
  const name = document.getElementById('form-name').value.trim();
  const phone = document.getElementById('form-phone').value.replace(/\D/g, '').slice(0, 10);
  const batch = document.getElementById('form-batch').value;
  const joinDate = document.getElementById('form-join-date').value || todayIso();
  if (!name || phone.length < 7) {
    showToast('Naam aur sahi phone number bharo!');
    return;
  }
  try {
    if (editingMemberId) {
      await updateDoc(doc(db, 'users', currentUid, 'members', editingMemberId), {
        name,
        phone,
        batch,
        joinDate,
        updatedAt: serverTimestamp()
      });
      showToast(`${name} update ho gaya!`);
    } else {
      const ref = doc(collection(db, 'users', currentUid, 'members'));
      await setDoc(ref, {
        userId: currentUid,
        name,
        phone,
        batch,
        joinDate,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp()
      });
      showToast(`${name} add ho gaya!`);
    }
    memberModal.classList.add('hidden');
  } catch (err) {
    showToast(err.message || 'Save nahi ho saka');
  }
});

document.getElementById('member-search').addEventListener('input', () => renderMembers());

document.getElementById('members-batch-chips').addEventListener('click', (e) => {
  const btn = e.target.closest('[data-batch]');
  if (!btn) return;
  memberBatchFilter = btn.dataset.batch;
  renderMembers();
});

const attDatePicker = document.getElementById('att-date-picker');
attDatePicker.value = selectedAttDate;
attDatePicker.addEventListener('change', () => {
  selectedAttDate = attDatePicker.value || todayIso();
  renderAttendance();
});

document.getElementById('att-batch-chips').addEventListener('click', (e) => {
  const btn = e.target.closest('[data-att-batch]');
  if (!btn) return;
  attBatchFilter = btn.dataset.attBatch;
  renderAttendance();
});

document.getElementById('mark-all-present-btn').addEventListener('click', () => markAllAttendance(true));
document.getElementById('mark-all-absent-btn').addEventListener('click', () => markAllAttendance(false));

document.getElementById('fees-prev-month').addEventListener('click', () => shiftFeesMonth(-1));
document.getElementById('fees-next-month').addEventListener('click', () => shiftFeesMonth(1));
document.getElementById('fees-filter-chips').addEventListener('click', (e) => {
  const btn = e.target.closest('[data-fee-filter]');
  if (!btn) return;
  feeFilter = btn.dataset.feeFilter;
  renderFees();
});

document.getElementById('fees-whatsapp-report-btn').addEventListener('click', () => {
  const feesMap = allFeeMonths[selectedFeesMonth] || {};
  const paid = members.filter((m) => feesMap[m.id]?.paid === true);
  const pending = members.filter((m) => feesMap[m.id]?.paid !== true);
  const msg = [
    `🏋️ *Avyukt Gym – Fees Report*`,
    `📅 ${formatMonthLabel(selectedFeesMonth)}`,
    ``,
    `👥 Total: ${members.length}`,
    `✅ Paid: ${paid.length} (₹${paid.length * FEES_AMOUNT})`,
    `❌ Pending: ${pending.length} (₹${pending.length * FEES_AMOUNT})`,
    paid.length ? `\n*Paid:*\n${paid.map((m) => `✅ ${m.name}`).join('\n')}` : '',
    pending.length ? `\n*Baaki:*\n${pending.map((m) => `❌ ${m.name}`).join('\n')}` : ''
  ].filter(Boolean).join('\n');
  window.open(`https://wa.me/${OWNER_PHONE_RAW}?text=${encodeURIComponent(msg)}`, '_blank', 'noopener');
});

function shiftFeesMonth(delta) {
  const [y, m] = selectedFeesMonth.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  selectedFeesMonth = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  renderFees();
}

async function setAttendance(memberId, present) {
  if (!currentUid) return;
  const docRef = doc(db, 'users', currentUid, 'attendance', selectedAttDate);
  try {
    await runTransaction(db, async (tx) => {
      const snap = await tx.get(docRef);
      if (!snap.exists()) {
        tx.set(docRef, {
          userId: currentUid,
          date: selectedAttDate,
          records: { [memberId]: present },
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      } else {
        tx.update(docRef, {
          [`records.${memberId}`]: present,
          updatedAt: serverTimestamp()
        });
      }
    });
    showToast(present ? 'Present mark kiya' : 'Absent mark kiya');
  } catch (err) {
    showToast(err.message || 'Attendance save error');
  }
}

async function markAllAttendance(present) {
  if (!currentUid) return;
  const targets = members.filter((m) => matchesBatch(m.batch, attBatchFilter));
  if (!targets.length) return;
  const docRef = doc(db, 'users', currentUid, 'attendance', selectedAttDate);
  try {
    await runTransaction(db, async (tx) => {
      const snap = await tx.get(docRef);
      if (!snap.exists()) {
        const records = {};
        targets.forEach((m) => { records[m.id] = present; });
        tx.set(docRef, {
          userId: currentUid,
          date: selectedAttDate,
          records,
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      } else {
        const records = { ...(snap.data()?.records || {}) };
        targets.forEach((m) => { records[m.id] = present; });
        tx.update(docRef, { records, updatedAt: serverTimestamp() });
      }
    });
    showToast(present ? 'Sabko Present kiya!' : 'Sabko Absent mark kiya');
  } catch (err) {
    showToast(err.message || 'Save error');
  }
}

async function setFeeStatus(month, memberId, paid) {
  if (!currentUid) return;
  const docRef = doc(db, 'users', currentUid, 'fees', month);
  const statusObj = paid ? { paid: true, paidDate: todayIso() } : { paid: false };
  try {
    await runTransaction(db, async (tx) => {
      const snap = await tx.get(docRef);
      if (!snap.exists()) {
        tx.set(docRef, {
          userId: currentUid,
          month,
          records: { [memberId]: statusObj },
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp()
        });
      } else {
        const records = { ...(snap.data()?.records || {}) };
        records[memberId] = statusObj;
        tx.update(docRef, { records, updatedAt: serverTimestamp() });
      }
    });
    showToast(paid ? 'Fees Paid mark ho gayi!' : 'Fees Pending mark ho gayi');
  } catch (err) {
    showToast(err.message || 'Fees save error');
  }
}

function downloadCsv(filename, rows) {
  const esc = (v) => {
    const s = String(v ?? '');
    return /[,"\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  const csv = rows.map((r) => r.map(esc).join(',')).join('\n');
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
  const a = document.createElement('a');
  a.href = URL.createObjectURL(blob);
  a.download = filename;
  a.click();
  URL.revokeObjectURL(a.href);
}

function exportMembersCsv() {
  downloadCsv(`avyukt_gym_members_backup_${todayIso()}.csv`, [
    ['Name', 'Phone', 'Batch', 'JoinDate'],
    ...members.map((m) => [m.name, m.phone, batchLabel(m.batch), m.joinDate])
  ]);
}

function exportFeesCsv() {
  const feesMap = allFeeMonths[selectedFeesMonth] || {};
  downloadCsv(`avyukt_gym_fees_${selectedFeesMonth}.csv`, [
    ['Name', 'Phone', 'Batch', 'Month', 'AmountINR', 'Status', 'PaidDate'],
    ...members.map((m) => {
      const f = feesMap[m.id];
      return [m.name, m.phone, batchLabel(m.batch), selectedFeesMonth, FEES_AMOUNT, f?.paid ? 'Fees Paid' : 'Fees Pending', f?.paidDate || ''];
    })
  ]);
}

function exportAttendanceCsv() {
  const prefix = selectedAttDate.slice(0, 7);
  const days = Object.keys(allAttendanceDays).filter((d) => d.startsWith(prefix)).sort();
  downloadCsv(`avyukt_gym_attendance_${prefix}.csv`, [
    ['Name', 'Phone', 'Batch', 'Month', 'DaysPresent', ...days],
    ...members.map((m) => [
      m.name,
      m.phone,
      batchLabel(m.batch),
      prefix,
      days.filter((d) => allAttendanceDays[d]?.[m.id] === true).length,
      ...days.map((d) => (allAttendanceDays[d]?.[m.id] === true ? 'P' : allAttendanceDays[d]?.[m.id] === false ? 'A' : '-'))
    ])
  ]);
}

document.getElementById('dash-export-fees').addEventListener('click', exportFeesCsv);
document.getElementById('dash-export-att').addEventListener('click', exportAttendanceCsv);
document.getElementById('dash-export-members').addEventListener('click', exportMembersCsv);
document.getElementById('members-export-btn').addEventListener('click', exportMembersCsv);
document.getElementById('att-export-btn').addEventListener('click', exportAttendanceCsv);
document.getElementById('fees-export-btn').addEventListener('click', exportFeesCsv);

const historyModal = document.getElementById('history-modal');
document.getElementById('history-close-btn').addEventListener('click', () => historyModal.classList.add('hidden'));

function openMemberHistory(member) {
  document.getElementById('history-member-name').textContent = member.name;
  document.getElementById('history-member-meta').textContent = `${member.phone} • ${batchLabel(member.batch)} • Joined ${formatDisplayDate(member.joinDate)}`;
  const container = document.getElementById('history-content');
  container.innerHTML = '';

  const months = Array.from({ length: 6 }, (_, i) => {
    const d = new Date();
    d.setMonth(d.getMonth() - i);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`;
  });

  months.forEach((ym) => {
    const fee = allFeeMonths[ym]?.[member.id];
    const isPaid = fee?.paid === true;
    const row = document.createElement('div');
    row.className = 'item-card';
    row.innerHTML = `
      <div class="item-main">
        <div class="item-title">${formatMonthLabel(ym)}</div>
        <div class="item-sub">${isPaid && fee?.paidDate ? `Paid on ${formatDisplayDate(fee.paidDate)}` : `₹${FEES_AMOUNT}`}</div>
      </div>
    `;
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `toggle-pill ${isPaid ? 'paid' : 'pending'}`;
    btn.textContent = isPaid ? 'Fees Paid' : 'Fees Pending';
    btn.addEventListener('click', () => setFeeStatus(ym, member.id, !isPaid).then(() => openMemberHistory(member)));
    row.appendChild(btn);
    container.appendChild(row);
  });

  historyModal.classList.remove('hidden');
}

function renderAll() {
  renderDashboard();
  renderMembers();
  renderAttendance();
  renderFees();
}

function renderDashboard() {
  document.getElementById('today-date-label').textContent = new Date().toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
  const todayAtt = allAttendanceDays[todayIso()] || {};
  const monthFees = allFeeMonths[currentMonthIso()] || {};
  const paidCount = members.filter((m) => monthFees[m.id]?.paid === true).length;
  const pendingCount = Math.max(0, members.length - paidCount);
  const presentToday = members.filter((m) => todayAtt[m.id] === true).length;

  document.getElementById('kpi-total-members').textContent = members.length;
  document.getElementById('kpi-today-present').textContent = presentToday;
  document.getElementById('kpi-fees-paid').textContent = `₹${(paidCount * FEES_AMOUNT).toLocaleString('en-IN')}`;
  document.getElementById('kpi-fees-pending').textContent = pendingCount;

  const dueAlerts = members
    .map((m) => ({ member: m, due: computeDueInfo(m.joinDate, monthFees[m.id]?.paid === true) }))
    .filter((x) => x.due.urgency === 'OVERDUE' || x.due.urgency === 'DUE_SOON')
    .sort((a, b) => a.due.daysRemaining - b.due.daysRemaining);

  const card = document.getElementById('due-alerts-card');
  const list = document.getElementById('due-alerts-list');
  list.innerHTML = '';
  card.classList.toggle('hidden', dueAlerts.length === 0);

  dueAlerts.slice(0, 4).forEach(({ member, due }) => {
    const label = due.daysRemaining < 0 ? `Overdue ${-due.daysRemaining}d` : due.daysRemaining === 0 ? 'Due Today' : `Due in ${due.daysRemaining}d`;
    const el = document.createElement('div');
    el.className = 'item-card';
    el.innerHTML = `
      <div class="item-main">
        <div class="item-title"></div>
        <div class="item-sub">₹${FEES_AMOUNT}</div>
      </div>
      <span class="pill ${due.urgency === 'OVERDUE' ? 'red' : 'orange'}">${label}</span>
    `;
    el.querySelector('.item-title').textContent = member.name;
    el.addEventListener('click', () => openMemberHistory(member));
    list.appendChild(el);
  });
}

function renderMembers() {
  document.getElementById('members-count-pill').textContent = members.length;
  const monthFees = allFeeMonths[currentMonthIso()] || {};
  const q = document.getElementById('member-search').value.trim().toLowerCase();

  document.querySelectorAll('#members-batch-chips .chip').forEach((c) => {
    const b = c.dataset.batch;
    const cnt = members.filter((m) => matchesBatch(m.batch, b)).length;
    c.textContent = `${b === 'ALL' ? 'All' : batchLabel(b)} (${cnt})`;
    c.classList.toggle('active', memberBatchFilter === b);
  });

  const filtered = members.filter((m) =>
    matchesBatch(m.batch, memberBatchFilter) &&
    (!q || m.name.toLowerCase().includes(q) || m.phone.includes(q))
  );

  const list = document.getElementById('members-list');
  list.innerHTML = '';
  if (!filtered.length) {
    list.innerHTML = '<div class="admin-card"><p class="muted">Koi member nahi mila. + Add button se member add karein.</p></div>';
    return;
  }

  filtered.forEach((m) => {
    const isPaid = monthFees[m.id]?.paid === true;
    const card = document.createElement('div');
    card.className = 'item-card';
    card.innerHTML = `
      <div class="item-main">
        <div class="item-title"></div>
        <div class="item-sub"></div>
        <div class="badge-row">
          <span class="pill blue">${batchLabel(m.batch)}</span>
          <span class="pill ${isPaid ? 'green' : 'red'}">${isPaid ? 'Paid' : 'Pending'}</span>
        </div>
      </div>
      <div class="item-actions">
        <button type="button" class="icon-btn edit-btn" aria-label="Edit">✎</button>
        <a class="icon-btn" href="tel:${m.phone}" aria-label="Call">📞</a>
        <button type="button" class="icon-btn del-btn" aria-label="Delete">🗑</button>
      </div>
    `;
    card.querySelector('.item-title').textContent = m.name;
    card.querySelector('.item-sub').textContent = m.phone;
    card.querySelector('.item-main').addEventListener('click', () => openMemberHistory(m));
    card.querySelector('.edit-btn').addEventListener('click', () => openMemberModal(m));
    card.querySelector('.del-btn').addEventListener('click', async () => {
      if (!confirm(`${m.name} ko delete karein?`)) return;
      await deleteDoc(doc(db, 'users', currentUid, 'members', m.id));
      showToast(`${m.name} delete ho gaya`);
    });
    list.appendChild(card);
  });
}

function renderAttendance() {
  const attMap = allAttendanceDays[selectedAttDate] || {};
  document.querySelectorAll('#att-batch-chips .chip').forEach((c) => {
    const b = c.dataset.attBatch;
    const cnt = members.filter((m) => matchesBatch(m.batch, b)).length;
    c.textContent = `${b === 'ALL' ? 'All' : batchLabel(b)} (${cnt})`;
    c.classList.toggle('active', attBatchFilter === b);
  });

  const filtered = members.filter((m) => matchesBatch(m.batch, attBatchFilter));
  const presentCount = filtered.filter((m) => attMap[m.id] === true).length;
  document.getElementById('att-present-count').textContent = presentCount;
  document.getElementById('att-absent-count').textContent = Math.max(0, filtered.length - presentCount);
  document.getElementById('att-total-count').textContent = filtered.length;

  const list = document.getElementById('attendance-list');
  list.innerHTML = '';
  if (!filtered.length) {
    list.innerHTML = '<div class="admin-card"><p class="muted">Is batch filter me koi member nahi hai.</p></div>';
    return;
  }

  filtered.forEach((m) => {
    const present = attMap[m.id] === true;
    const card = document.createElement('div');
    card.className = 'item-card';
    card.innerHTML = `
      <div class="item-main">
        <div class="item-title"></div>
        <div class="item-sub"></div>
      </div>
    `;
    card.querySelector('.item-title').textContent = m.name;
    card.querySelector('.item-sub').textContent = `${m.phone} • ${batchLabel(m.batch)}`;
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.className = `toggle-pill ${present ? 'present' : 'absent'}`;
    btn.textContent = present ? 'Present' : 'Absent';
    btn.addEventListener('click', () => setAttendance(m.id, !present));
    card.appendChild(btn);
    list.appendChild(card);
  });
}

function renderFees() {
  document.getElementById('fees-month-label').textContent = formatMonthLabel(selectedFeesMonth);
  const feesMap = allFeeMonths[selectedFeesMonth] || {};
  const paidCount = members.filter((m) => feesMap[m.id]?.paid === true).length;
  const pendingCount = Math.max(0, members.length - paidCount);

  document.querySelectorAll('#fees-filter-chips .chip').forEach((c) => {
    const f = c.dataset.feeFilter;
    const cnt = f === 'ALL' ? members.length : f === 'PAID' ? paidCount : pendingCount;
    const label = f === 'ALL' ? 'All' : f === 'PAID' ? 'Fees Paid' : 'Fees Pending';
    c.textContent = `${label} (${cnt})`;
    c.classList.toggle('active', feeFilter === f);
  });

  const displayed = members
    .filter((m) => {
      const p = feesMap[m.id]?.paid === true;
      return feeFilter === 'ALL' || (feeFilter === 'PAID' ? p : !p);
    })
    .sort((a, b) => (feesMap[a.id]?.paid === true ? 1 : 0) - (feesMap[b.id]?.paid === true ? 1 : 0));

  const list = document.getElementById('fees-list');
  list.innerHTML = '';
  if (!displayed.length) {
    list.innerHTML = '<div class="admin-card"><p class="muted">Is filter me koi member nahi hai.</p></div>';
    return;
  }

  displayed.forEach((m) => {
    const fee = feesMap[m.id];
    const isPaid = fee?.paid === true;
    const card = document.createElement('div');
    card.className = 'item-card';
    card.innerHTML = `
      <div class="item-main">
        <div class="item-title"></div>
        <div class="item-sub"></div>
      </div>
      <div class="item-actions"></div>
    `;
    card.querySelector('.item-title').textContent = m.name;
    card.querySelector('.item-sub').textContent = isPaid && fee?.paidDate
      ? `${batchLabel(m.batch)} • Paid ${formatDisplayDate(fee.paidDate)}`
      : `${batchLabel(m.batch)} • ₹${FEES_AMOUNT} baaki`;

    const actions = card.querySelector('.item-actions');
    if (!isPaid) {
      const digits = (m.phone || '').replace(/\D/g, '');
      const wa = digits.length === 10 ? `91${digits}` : digits;
      const msg = `Namaste ${m.name} ji, Avyukt Gym ki ${formatMonthLabel(selectedFeesMonth)} mahine ki fees (₹${FEES_AMOUNT}) pending hai. Kripya jaldi jama kar dein. Dhanyavaad!`;
      const remindLink = document.createElement('a');
      remindLink.className = 'btn-whatsapp-sm small';
      remindLink.href = `https://wa.me/${wa}?text=${encodeURIComponent(msg)}`;
      remindLink.target = '_blank';
      remindLink.rel = 'noopener';
      remindLink.textContent = 'Remind';
      actions.appendChild(remindLink);
    }

    const toggleBtn = document.createElement('button');
    toggleBtn.type = 'button';
    toggleBtn.className = `toggle-pill ${isPaid ? 'paid' : 'pending'}`;
    toggleBtn.textContent = isPaid ? 'Fees Paid' : 'Fees Pending';
    toggleBtn.addEventListener('click', () => setFeeStatus(selectedFeesMonth, m.id, !isPaid));
    actions.appendChild(toggleBtn);
    list.appendChild(card);
  });
}
