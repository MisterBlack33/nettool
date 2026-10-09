(function () {
  const data = JSON.parse(document.getElementById('coverageData').textContent);
  const PRESETS = [7, 10, 20, 50];
  const MIN_RUNS = 2;
  const MAX_X_LABELS = 12;
  const SECONDS_PER_MINUTE = 60;
  const SECONDS_PER_HOUR = 3600;
  const POINT_RADIUS = 3.5;
  const dim = data.dim;
  const plotWidth = dim.w - dim.ml - dim.mr;
  const plotHeight = dim.h - dim.mt - dim.mb;
  const total = data.runs.length;

  function escapeAttr(text) {
    return String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
        .replace(/'/g, '&#39;').replace(/"/g, '&quot;');
  }

  function formatRuntime(seconds) {
    if (seconds >= SECONDS_PER_HOUR) {
      return (seconds / SECONDS_PER_HOUR).toFixed(1) + ' h (' + (seconds / SECONDS_PER_MINUTE).toFixed(1) + ' min)';
    }
    if (seconds >= SECONDS_PER_MINUTE) {
      return (seconds / SECONDS_PER_MINUTE).toFixed(1) + ' min (' + Math.round(seconds) + ' s)';
    }
    return seconds.toFixed(1) + ' s';
  }

  const round1 = (value) => Math.round(value * 10) / 10;
  const xAt = (index, count) => round1(dim.ml + plotWidth * index / Math.max(1, count - 1));
  const yAt = (value) => round1(dim.mt + plotHeight * (1 - value));
  const percent = (value) => round1(value * 100);
  const colorAt = (index) => data.palette[index % data.palette.length];

  function gridSvg() {
    const right = dim.ml + plotWidth;
    const parts = [0, 0.25, 0.5, 0.75, 1].map(function (p) {
      const y = yAt(p);
      return "<line x1='" + dim.ml + "' y1='" + y + "' x2='" + right + "' y2='" + y + "' stroke='#2a2f2c'/>"
          + "<text x='" + (dim.ml - 6) + "' y='" + (y + 4) + "' text-anchor='end' class='t'>" + percent(p) + '%</text>';
    });
    const ty = yAt(data.threshold);
    parts.push("<line x1='" + dim.ml + "' y1='" + ty + "' x2='" + right + "' y2='" + ty
        + "' stroke='#e05a4a' stroke-dasharray='6 4'/>");
    parts.push("<text x='" + right + "' y='" + (ty - 4) + "' text-anchor='end' fill='#e05a4a' class='t'>Ziel "
        + percent(data.threshold) + '%</text>');
    return parts.join('\n');
  }

  function labelsSvg(labels) {
    const step = Math.max(1, Math.ceil(labels.length / MAX_X_LABELS));
    const y = dim.h - dim.mb + dim.mt - 2;
    const parts = [];
    for (let i = 0; i < labels.length; i += step) {
      parts.push("<text x='" + xAt(i, labels.length) + "' y='" + y + "' text-anchor='middle' class='t'>"
          + escapeAttr(labels[i]) + '</text>');
    }
    return parts.join('\n');
  }

  function lineSvg(values, color, view) {
    const points = [];
    values.forEach(function (value, i) {
      if (value === null) return;
      const runtime = view.runtimes[i];
      const runtimeText = runtime === null ? '' : ' | Laufzeit: ' + formatRuntime(runtime);
      const tip = escapeAttr(view.labels[i] + ' | Wert: ' + percent(value) + '%' + runtimeText);
      points.push({ x: xAt(i, values.length), y: yAt(value), tip: tip });
    });
    if (!points.length) return '';
    const poly = points.map((p) => p.x + ',' + p.y).join(' ');
    const dots = points.map((p) => "<circle cx='" + p.x + "' cy='" + p.y + "' r='" + POINT_RADIUS + "' fill='" + color
        + "' class='data-point' data-tooltip='" + p.tip + "' role='img' aria-label='" + p.tip + "'/>").join('\n');
    return "<polyline points='" + poly + "' fill='none' stroke='" + color + "' stroke-width='2'/>\n" + dots;
  }

  function notesSvg(view) {
    return data.notes.map(function (note) {
      const index = view.runs.indexOf(note.run);
      if (index < 0 || !note.comment.trim()) return '';
      const x = xAt(index, view.runs.length);
      const tip = escapeAttr(note.comment);
      const y2 = dim.mt + plotHeight;
      return "<line x1='" + x + "' y1='" + dim.mt + "' x2='" + x + "' y2='" + y2
          + "' stroke='transparent' stroke-width='12' pointer-events='stroke' class='note-marker-hit' data-tooltip='"
          + tip + "' tabindex='0' role='img' aria-label='" + tip + "'/>"
          + "<line x1='" + x + "' y1='" + dim.mt + "' x2='" + x + "' y2='" + y2
          + "' stroke='#f7e000' stroke-width='2' stroke-dasharray='7 5' pointer-events='none'/>";
    }).join('\n');
  }

  function legendHtml(series) {
    return Object.keys(series).map(function (name, i) {
      const known = series[name].filter((v) => v !== null);
      const last = known.length ? percent(known[known.length - 1]) + '%' : '-';
      return "<span><i style='background:" + colorAt(i) + "'></i>" + escapeAttr(name) + ' <b>' + last + '</b></span>';
    }).join('\n');
  }

  function viewOf(count, offset) {
    const end = total - offset;
    const start = end - count;
    return {
      runs: data.runs.slice(start, end), labels: data.labels.slice(start, end),
      runtimes: data.runtimes.slice(start, end),
      slice: function (series) {
        const out = {};
        Object.keys(series).forEach((name) => { out[name] = series[name].slice(start, end); });
        return out;
      }
    };
  }

  function renderChart(svgId, legendId, series, view) {
    const lines = Object.keys(series).map((name, i) => lineSvg(series[name], colorAt(i), view)).join('\n');
    document.getElementById(svgId).innerHTML =
        [gridSvg(), labelsSvg(view.labels), lines, notesSvg(view)].join('\n');
    document.getElementById(legendId).innerHTML = legendHtml(series);
  }

  function describe(count, offset) {
    if (offset > 0) return count + ' Tests, bis vor ' + offset + ' Tests';
    return count >= total ? 'alle Tests (' + total + ')' : 'letzte ' + count + ' Tests';
  }

  const state = { count: total, offset: 0 };

  function render(count, offset) {
    const maxOffset = total - count;
    state.count = count;
    state.offset = Math.min(offset, maxOffset);
    const view = viewOf(state.count, state.offset);
    renderChart('chartTotal', 'legendTotal', view.slice(data.total), view);
    renderChart('chartPackages', 'legendPackages', view.slice(data.packages), view);
    const text = describe(state.count, state.offset);
    document.getElementById('rangeLabel').textContent = text;
    document.querySelectorAll('[data-range-tag]').forEach((node) => { node.textContent = text; });
    slider.value = state.count;
    offsetSlider.max = maxOffset;
    offsetSlider.value = state.offset;
    offsetSlider.disabled = maxOffset === 0;
  }

  function buildPresets() {
    const box = document.getElementById('rangePresets');
    const counts = PRESETS.filter((n) => n < total).concat([total]);
    counts.forEach(function (count) {
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = count === total ? 'Alle' : String(count);
      button.addEventListener('click', () => render(count, state.offset));
      box.appendChild(button);
    });
  }

  const slider = document.getElementById('rangeSlider');
  const offsetSlider = document.getElementById('offsetSlider');
  slider.min = Math.min(MIN_RUNS, total);
  slider.max = total;
  offsetSlider.min = 0;
  slider.addEventListener('input', () => render(Number(slider.value), state.offset));
  offsetSlider.addEventListener('input', () => render(state.count, Number(offsetSlider.value)));
  buildPresets();
  render(total, 0);
})();