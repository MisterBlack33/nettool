(function () {
  const OFFSET = 12;
  const tooltip = document.getElementById('chartTooltip');
  if (!tooltip) return;

  function targetOf(event) {
    return event.target.closest ? event.target.closest('.data-point, .note-marker-hit') : null;
  }

  function show(event, text) {
    tooltip.textContent = text;
    const overflow = event.clientX + OFFSET + tooltip.offsetWidth > window.innerWidth;
    const left = overflow ? event.clientX - tooltip.offsetWidth - OFFSET : event.clientX + OFFSET;
    tooltip.style.left = Math.max(0, left) + 'px';
    tooltip.style.top = (event.clientY + OFFSET) + 'px';
    tooltip.style.opacity = '1';
  }

  function hide() { tooltip.style.opacity = '0'; }

  document.addEventListener('mousemove', function (event) {
    const node = targetOf(event);
    if (node && node.getAttribute('data-tooltip')) show(event, node.getAttribute('data-tooltip'));
    else hide();
  });
  document.addEventListener('focusin', function (event) {
    const node = targetOf(event);
    if (node) show({ clientX: 0, clientY: 0 }, node.getAttribute('data-tooltip'));
  });
  document.addEventListener('focusout', hide);
})();

