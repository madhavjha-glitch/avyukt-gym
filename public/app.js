(function () {
  const statusEl = document.getElementById('gym-status');
  const timeEl = document.getElementById('live-time');
  const installBanner = document.getElementById('install-banner');
  const installHint = document.getElementById('install-hint');
  const installBtn = document.getElementById('install-btn');
  const heroInstallBtn = document.getElementById('hero-install-btn');
  const closeBannerBtn = document.getElementById('close-banner');

  let deferredPrompt = null;

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

  if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
      navigator.serviceWorker.register('/sw.js').catch(() => {});
    });
  }

  const isStandalone =
    window.matchMedia('(display-mode: standalone)').matches ||
    window.navigator.standalone === true;

  function showInstallUi() {
    if (isStandalone) return;
    if (installBanner) installBanner.classList.remove('hidden');
    if (heroInstallBtn && deferredPrompt) heroInstallBtn.classList.remove('hidden');
  }

  function hideInstallUi() {
    if (installBanner) installBanner.classList.add('hidden');
    if (heroInstallBtn) heroInstallBtn.classList.add('hidden');
  }

  async function triggerInstall() {
    if (!deferredPrompt) return;
    deferredPrompt.prompt();
    const choice = await deferredPrompt.userChoice;
    deferredPrompt = null;
    if (choice && choice.outcome === 'accepted') {
      hideInstallUi();
    }
  }

  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    deferredPrompt = event;
    showInstallUi();
  });

  window.addEventListener('appinstalled', () => {
    deferredPrompt = null;
    hideInstallUi();
  });

  if (installBtn) {
    installBtn.addEventListener('click', triggerInstall);
  }
  if (heroInstallBtn) {
    heroInstallBtn.addEventListener('click', triggerInstall);
  }
  if (closeBannerBtn) {
    closeBannerBtn.addEventListener('click', () => {
      if (installBanner) installBanner.classList.add('hidden');
    });
  }

  const isIos = /iphone|ipad|ipod/i.test(window.navigator.userAgent);
  if (isIos && !isStandalone && installBanner && installHint && installBtn) {
    installHint.textContent = 'Safari Share button dabakar "Add to Home Screen" karein';
    installBtn.classList.add('hidden');
    installBanner.classList.remove('hidden');
  }
})();
