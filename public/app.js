(function () {
  const statusEl = document.getElementById('gym-status');
  const timeEl = document.getElementById('live-time');

  function updateGymStatus() {
    const now = new Date();
    const istString = now.toLocaleTimeString('en-IN', {
      timeZone: 'Asia/Kolkata',
      hour12: true,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit'
    });
    if (timeEl) {
      timeEl.textContent = istString + ' IST';
    }

    const istParts = new Intl.DateTimeFormat('en-US', {
      timeZone: 'Asia/Kolkata',
      weekday: 'short',
      hour: 'numeric',
      hour12: false
    }).formatToParts(now);

    const weekday = istParts.find((p) => p.type === 'weekday')?.value || '';
    const hour = Number(istParts.find((p) => p.type === 'hour')?.value || '0');

    const isSunday = weekday === 'Sun';
    const isMorning = hour >= 5 && hour < 10;
    const isEvening = hour >= 17 && hour < 22;

    if (!statusEl) return;

    if (isSunday) {
      statusEl.textContent = 'Sunday – Gym Off';
      statusEl.className = 'gym-status closed';
    } else if (isMorning || isEvening) {
      statusEl.textContent = 'Gym is OPEN Now!';
      statusEl.className = 'gym-status open';
    } else if (hour < 5) {
      statusEl.textContent = 'Opens at 5:00 AM';
      statusEl.className = 'gym-status closed';
    } else if (hour >= 10 && hour < 17) {
      statusEl.textContent = 'Reopens at 5:00 PM';
      statusEl.className = 'gym-status closed';
    } else {
      statusEl.textContent = 'Gym is Closed';
      statusEl.className = 'gym-status closed';
    }
  }

  updateGymStatus();
  setInterval(updateGymStatus, 1000);
})();
