import hljs from 'highlight.js';
import java from 'highlight.js/lib/languages/java';
import 'highlight.js/styles/github.css';

hljs.registerLanguage('java', java);

export default class HighlightSyntax {
  deep = true
  bind(el, binding) {
    let targets = el.querySelectorAll('code');
    targets.forEach((target) => {
      if (binding.value) {
        target.textContent = binding.value;
      }
      hljs.highlightBlock(target);
    });
  }
  componentUpdated(el, binding) {
    let targets = el.querySelectorAll('code');
    targets.forEach((target) => {
      if (binding.value) {
        target.textContent = binding.value;
        hljs.highlightBlock(target);
      }
    });
  }
}