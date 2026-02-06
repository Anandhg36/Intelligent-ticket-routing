import { Component, Input } from '@angular/core';

@Component({
  selector: 'rmit-loader',
  standalone: true,
  templateUrl: './rmit-loader.component.html',
  styleUrls: ['./rmit-loader.component.css']
})
export class RmitLoaderComponent {
  /** Size in px (affects the whole spinner) */
  @Input() size = 96;

  /** Brand color for strokes / bubbles (CSS color) */
  @Input() color = '#9F86C0'; // Lavender-rose primary

  /** Optional accessible label text */
  @Input() label = 'Loading…';

  /** Fullscreen overlay when true */
  @Input() overlay = false;
}
