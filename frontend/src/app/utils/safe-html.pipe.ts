import { Pipe, PipeTransform, inject } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import DOMPurify from 'dompurify';

@Pipe({ name: 'safeHtml', standalone: true })
export class SafeHtmlPipe implements PipeTransform {
  private sanitizer = inject(DomSanitizer);

  transform(html: string): SafeHtml {
    const clean = DOMPurify.sanitize(html, {
      ALLOWED_TAGS: [
        'p', 'br', 'strong', 'em', 'u', 's', 'span', 'a',
        'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
        'ul', 'ol', 'li', 'blockquote', 'pre', 'code',
        'img', 'sub', 'sup'
      ],
      ALLOWED_ATTR: ['class', 'style', 'href', 'target', 'rel', 'src', 'alt'],
    });

    const doc = new DOMParser().parseFromString(clean, 'text/html');
    doc.querySelectorAll<HTMLElement>('[style]').forEach(el => {
      el.style.removeProperty('white-space');
      el.style.removeProperty('word-break');
      el.style.removeProperty('overflow-wrap');
      el.style.removeProperty('width');
      el.style.removeProperty('min-width');
    });

    // Quill v2 getSemanticHTML() replaces every space with &nbsp; (U+00A0),
    // which has no word-wrap break opportunity. Restore regular spaces so
    // CSS white-space: normal can wrap lines at word boundaries.
    const walker = doc.createTreeWalker(doc.body, NodeFilter.SHOW_TEXT);
    let textNode = walker.nextNode() as Text | null;
    while (textNode) {
      if (textNode.textContent?.includes(' ')) {
        textNode.textContent = textNode.textContent.replace(/ /g, ' ');
      }
      textNode = walker.nextNode() as Text | null;
    }

    return this.sanitizer.bypassSecurityTrustHtml(doc.body.innerHTML);
  }
}
