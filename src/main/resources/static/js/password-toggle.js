/* Botão de olho: mostra ou oculta a senha que está sendo digitada. */
(function () {
  'use strict';

  function setVisible(button, visible) {
    var input = button.closest('.password-field, .auth-field').querySelector('input');
    input.type = visible ? 'text' : 'password';
    button.setAttribute('aria-pressed', String(visible));
    button.setAttribute('aria-label', visible ? 'Ocultar senha' : 'Mostrar senha');
    return input;
  }

  document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-password-toggle]');
    if (!button) return;
    var show = button.getAttribute('aria-pressed') !== 'true';
    setVisible(button, show).focus();
  });

  // Ao fechar um popup a senha volta a ficar oculta, para não aparecer na próxima vez que ele abrir.
  document.addEventListener('toggle', function (event) {
    if (event.newState !== 'closed') return;
    event.target.querySelectorAll('[data-password-toggle]').forEach(function (button) {
      setVisible(button, false);
    });
  }, true);
})();
