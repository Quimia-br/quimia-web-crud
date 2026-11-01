/* Tema claro/escuro: a escolha (claro, escuro ou do sistema) fica no navegador (localStorage). Roda na <head>, antes da
   página aparecer, para não piscar o tema errado; o resultado vai no atributo data-theme do <html> (o CSS troca as cores). */
(function () {
  'use strict';

  var KEY = 'quimia.theme';
  var root = document.documentElement;
  var dark = window.matchMedia ? window.matchMedia('(prefers-color-scheme: dark)') : null;

  /** 'light', 'dark' ou 'system' (padrão: acompanha o sistema operacional). */
  function stored() {
    try {
      var value = localStorage.getItem(KEY);
      return value === 'light' || value === 'dark' ? value : 'system';
    } catch (e) {
      return 'system';
    }
  }

  function resolve(choice) {
    if (choice === 'system') return dark && dark.matches ? 'dark' : 'light';
    return choice;
  }

  function markChoice(choice) {
    document.querySelectorAll('[data-theme-option]').forEach(function (input) {
      input.checked = input.value === choice;
    });
  }

  function apply(choice) {
    root.setAttribute('data-theme', resolve(choice));
    markChoice(choice);
  }

  apply(stored());

  document.addEventListener('DOMContentLoaded', function () {
    markChoice(stored());
  });

  if (dark && dark.addEventListener) {
    dark.addEventListener('change', function () {
      if (stored() === 'system') apply('system');
    });
  }

  document.addEventListener('change', function (event) {
    var input = event.target.closest('[data-theme-option]');
    if (!input) return;
    try {
      if (input.value === 'system') localStorage.removeItem(KEY);
      else localStorage.setItem(KEY, input.value);
    } catch (e) {}
    apply(input.value);
  });
})();
