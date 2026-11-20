/* Boot the offline desktop. */
(function () {
  const Win11 = window.Win11;

  function start() {
    Win11.Store.load();
    Win11.ui.init();
    document.title = `Windows 11 · ${Win11.userName()}`;
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', start);
  } else {
    start();
  }
})();
