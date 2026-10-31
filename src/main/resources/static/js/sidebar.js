/* Botão "Fixar menu": lembra no navegador (localStorage) se a barra lateral fica aberta. */
(function () {
  'use strict';

  var KEY = 'quimia.sidebarPinned';
  var root = document.documentElement;

  function isPinned() {
    try {
      return localStorage.getItem(KEY) === 'true';
    } catch (e) {
      return false;
    }
  }

  function apply(pinned) {
    root.classList.toggle('sidebar-pinned', pinned);
    document.querySelectorAll('[data-sidebar-pin]').forEach(function (button) {
      button.setAttribute('aria-pressed', String(pinned));
    });
  }

  root.classList.toggle('sidebar-pinned', isPinned());

  document.addEventListener('DOMContentLoaded', function () {
    apply(isPinned());
  });

  document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-sidebar-pin]');
    if (!button) return;
    var pinned = !root.classList.contains('sidebar-pinned');
    try {
      localStorage.setItem(KEY, String(pinned));
    } catch (e) {}
    apply(pinned);
  });
})();
