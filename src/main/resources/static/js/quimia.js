/* Quimia CRUD — page behaviour (table filter, delete/undo confirmation, change diff, mobile menu) */
(function () {
  'use strict';

  // Client-side table filter: <input data-filter="#tableId" data-count="#counterId">
  document.querySelectorAll('input[data-filter]').forEach(function (input) {
    var table = document.querySelector(input.getAttribute('data-filter'));
    if (!table) return;
    var rows = Array.prototype.slice.call(table.querySelectorAll('tbody tr[data-row]'));
    var counter = document.querySelector(input.getAttribute('data-count') || '#__none');
    var noResult = table.querySelector('tbody tr[data-no-result]');
    function apply() {
      var q = input.value.trim().toLowerCase();
      var visible = 0;
      rows.forEach(function (tr) {
        var chip = table.getAttribute('data-chip') || '';
        var show = (!q || tr.textContent.toLowerCase().indexOf(q) !== -1) && (!chip || tr.getAttribute('data-action') === chip);
        tr.hidden = !show;
        if (show) visible++;
      });
      if (counter) counter.textContent = visible + (visible === 1 ? ' registro' : ' registros');
      if (noResult) noResult.hidden = !(rows.length && visible === 0);
    }
    input.addEventListener('input', apply);
    apply();
  });

  // Delete confirmation: <button data-delete-url="/x/1/delete" data-delete-name="Name">
  var dialog = document.getElementById('confirm-delete');
  if (dialog && typeof dialog.showModal === 'function') {
    var form = dialog.querySelector('form.confirm-form');
    var nameEl = dialog.querySelector('[data-confirm-name]');
    document.addEventListener('click', function (ev) {
      var btn = ev.target.closest('[data-delete-url]');
      if (!btn) return;
      ev.preventDefault();
      form.setAttribute('action', btn.getAttribute('data-delete-url'));
      nameEl.textContent = btn.getAttribute('data-delete-name') || 'este registro';
      dialog.showModal();
    });
    dialog.querySelector('[data-confirm-cancel]').addEventListener('click', function () { dialog.close(); });
    form.addEventListener('submit', function () {
      var submit = form.querySelector('button[type=submit]');
      submit.disabled = true;
      submit.textContent = 'Excluindo…';
    });
  }


  // Chip filter: <div data-chip-filter="#tableId"> with data-value buttons; rows carry data-action
  document.querySelectorAll('[data-chip-filter]').forEach(function (group) {
    var table = document.querySelector(group.getAttribute('data-chip-filter'));
    var search = document.querySelector('input[data-filter="' + group.getAttribute('data-chip-filter') + '"]');
    if (!table) return;
    group.addEventListener('click', function (ev) {
      var chip = ev.target.closest('.chip');
      if (!chip) return;
      group.querySelectorAll('.chip').forEach(function (c) { c.classList.toggle('active', c === chip); c.setAttribute('aria-pressed', String(c === chip)); });
      table.setAttribute('data-chip', chip.getAttribute('data-value'));
      if (search) search.dispatchEvent(new Event('input'));
    });
  });

  // Undo confirmation: <button data-undo-url="/admin-logs/1/undo" data-undo-msg="...">
  var undo = document.getElementById('confirm-undo');
  if (undo && typeof undo.showModal === 'function') {
    var uform = undo.querySelector('form.undo-form');
    var utext = undo.querySelector('[data-undo-text]');
    document.addEventListener('click', function (ev) {
      var btn = ev.target.closest('[data-undo-url]');
      if (!btn) return;
      ev.preventDefault();
      uform.setAttribute('action', btn.getAttribute('data-undo-url'));
      utext.textContent = btn.getAttribute('data-undo-msg') || 'Esta alteração será revertida.';
      undo.showModal();
    });
    undo.querySelector('[data-undo-cancel]').addEventListener('click', function () { undo.close(); });
    uform.addEventListener('submit', function () {
      var b = uform.querySelector('button[type=submit]');
      b.disabled = true; b.textContent = 'Desfazendo…';
    });
  }

  // Before × after comparison: <div id="diff" data-before="{json}" data-after="{json}">
  var diff = document.getElementById('diff');
  if (diff) {
    var parse = function (txt) {
      if (txt == null || txt === '') return null;
      try { var v = JSON.parse(txt); return (v && typeof v === 'object' && !Array.isArray(v)) ? v : undefined; }
      catch (e) { return undefined; }
    };
    var rawBefore = diff.getAttribute('data-before'), rawAfter = diff.getAttribute('data-after');
    var a = parse(rawBefore), d = parse(rawAfter);
    if (a !== undefined && d !== undefined && (a || d)) {
      a = a || {}; d = d || {};
      var keys = Object.keys(a);
      Object.keys(d).forEach(function (k) { if (keys.indexOf(k) === -1) keys.push(k); });
      var fmt = function (v) { return v === undefined ? '' : (typeof v === 'object' && v !== null ? JSON.stringify(v) : String(v)); };
      var table = document.createElement('table');
      table.className = 'diff-table';
      table.innerHTML = '<thead><tr><th>Campo</th><th>Antes</th><th>Depois</th></tr></thead>';
      var tbody = document.createElement('tbody');
      var same = 0;
      keys.forEach(function (k) {
        var inA = Object.prototype.hasOwnProperty.call(a, k), inD = Object.prototype.hasOwnProperty.call(d, k);
        var va = fmt(a[k]), vd = fmt(d[k]);
        var state = !inA ? 'added' : !inD ? 'removed' : (va === vd ? 'same' : 'changed');
        if (state === 'same') same++;
        var tr = document.createElement('tr');
        tr.className = state;
        [k, va, vd].forEach(function (txt, i) {
          var td = document.createElement('td');
          if (i === 1) td.className = 'old'; if (i === 2) td.className = 'new';
          if (i > 0 && txt === '') { td.classList.add('empty-val'); txt = '—'; }
          td.textContent = txt;
          tr.appendChild(td);
        });
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      diff.innerHTML = '';
      diff.appendChild(table);
      if (same > 0 && same < keys.length) {
        var label = document.createElement('label');
        label.className = 'diff-toggle';
        label.innerHTML = '<input type="checkbox"> Mostrar só campos alterados';
        label.querySelector('input').addEventListener('change', function (ev) {
          tbody.querySelectorAll('tr.same').forEach(function (tr) { tr.hidden = ev.target.checked; });
        });
        diff.appendChild(label);
      }
    }
  }

  // Mobile sidebar
  var toggle = document.querySelector('.menu-toggle');
  var sidebar = document.querySelector('.sidebar');
  if (toggle && sidebar) {
    toggle.addEventListener('click', function () {
      var open = sidebar.classList.toggle('open');
      toggle.setAttribute('aria-expanded', String(open));
    });
    document.addEventListener('click', function (ev) {
      if (sidebar.classList.contains('open') && !sidebar.contains(ev.target) && !toggle.contains(ev.target)) {
        sidebar.classList.remove('open');
        toggle.setAttribute('aria-expanded', 'false');
      }
    });
  }

  // Auto-hide success alerts
  document.querySelectorAll('.alert[data-autohide]').forEach(function (el) {
    setTimeout(function () { el.style.transition = 'opacity .3s'; el.style.opacity = '0'; setTimeout(function () { el.remove(); }, 300); }, 5000);
  });
})();
