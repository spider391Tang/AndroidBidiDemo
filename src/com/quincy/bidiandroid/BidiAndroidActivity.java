package com.quincy.bidiandroid;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;

import java.awt.font.TextAttribute;
import java.text.AttributedString;
//import java.text.Bidi;

import com.ibm.icu.text.Bidi;
import com.ibm.icu.text.BidiRun;

public class BidiAndroidActivity extends Activity {
	private static final String TAG = "BidiAndroidActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		EditText et = (EditText) findViewById(R.id.bidiEditText);
		et.setText("I bidi have פורשה 80 ");

		
		render_start();

	}

	static final int styleNormal = 0;
	static final int styleSelected = 1;
	static final int styleBold = 2;
	static final int styleItalics = 4;
	static final int styleSuper=8;
	static final int styleSub = 16;

	static class StyleRun {
		int limit;
		int style;

		public StyleRun(int limit, int style) {
			this.limit = limit;
			this.style = style;
		}
	}

	static class Bounds {
		int start;
		int limit;

		public Bounds(int start, int limit) {
			this.start = start;
			this.limit = limit;
		}
	}

	static int getTextWidth(String text, int start, int limit,
			StyleRun[] styleRuns, int styleRunCount) {
		// simplistic way to compute the width
		Log.v(TAG, "getTextWidth");
		return limit - start;
	}

	// set limit and StyleRun limit for a line
	// from text[start] and from styleRuns[styleRunStart]
	// using Bidi.getLogicalRun(...)
	// returns line width
	static int getLineBreak(String text, Bounds line, Bidi para,
			StyleRun styleRuns[], Bounds styleRun) {
		// dummy return
		Log.v(TAG, "getLineBreak");
		return 0;
	}

	// render runs on a line sequentially, always from left to right

	// prepare rendering a new line
	static void startLine(byte textDirection, int lineWidth) {
		System.out.println();
		Log.v(TAG, "startLine");
	}

	// render a run of text and advance to the right by the run width
	// the text[start..limit-1] is always in logical order
	static void renderRun(String text, int start, int limit,
			byte textDirection, int style) {

		Log.v(TAG, "renderRun");
	}

	// We could compute a cross-product
	// from the style runs with the directional runs
	// and then reorder it.
	// Instead, here we iterate over each run type
	// and render the intersections -
	// with shortcuts in simple (and common) cases.
	// renderParagraph() is the main function.

	// render a directional run with
	// (possibly) multiple style runs intersecting with it
	static void renderDirectionalRun(String text, int start, int limit,
			byte direction, StyleRun styleRuns[],
			int styleRunCount) {
		Log.v(TAG, "renderDirectionalRun");
		int i;

		// iterate over style runs
		if (direction == Bidi.LTR) {
			int styleLimit;
			for (i = 0; i < styleRunCount; ++i) {
				styleLimit = styleRuns[i].limit;
				if (start < styleLimit) {
					if (styleLimit > limit) {
						styleLimit = limit;
					}
					renderRun(text, start, styleLimit,
							direction, styleRuns[i].style);
					if (styleLimit == limit) {
						break;
					}
					start = styleLimit;
				}
			}
		} else {
			int styleStart;

			for (i = styleRunCount-1; i >= 0; --i) {
				if (i > 0) {
					styleStart = styleRuns[i-1].limit;
				} else {
					styleStart = 0;
				}
				if (limit >= styleStart) {
					if (styleStart < start) {
						styleStart = start;
					}
					renderRun(text, styleStart, limit, direction,
							styleRuns[i].style);
					if (styleStart == start) {
						break;
					}
					limit = styleStart;
				}
			}
		}
	}

	// the line object represents text[start..limit-1]
	static void renderLine(Bidi line, String text, int start, int limit,
			StyleRun styleRuns[], int styleRunCount) {
		Log.v(TAG, "renderLine");
		byte direction = line.getDirection();
		if (direction != Bidi.MIXED) {
			// unidirectional
			if (styleRunCount <= 1) {
				renderRun(text, start, limit, direction, styleRuns[0].style);
			} else {
				renderDirectionalRun(text, start, limit, direction,
						styleRuns, styleRunCount);
			}
		} else {
			// mixed-directional
			int count, i;
			BidiRun run;

			try {
				count = line.countRuns();
			} catch (IllegalStateException e) {
				e.printStackTrace();
				return;
			}
			if (styleRunCount <= 1) {
				int style = styleRuns[0].style;

				// iterate over directional runs
				for (i = 0; i < count; ++i) {
					run = line.getVisualRun(i);
					renderRun(text, run.getStart(), run.getLimit(),
							run.getDirection(), style);
				}
			} else {
				// iterate over both directional and style runs
				for (i = 0; i < count; ++i) {
					run = line.getVisualRun(i);
					renderDirectionalRun(text, run.getStart(),
							run.getLimit(), run.getDirection(),
							styleRuns, styleRunCount);
				}
			}
		}
	}

	static void renderParagraph(String text, byte textDirection,
			StyleRun styleRuns[], int styleRunCount,
			int lineWidth) {
		Log.v(TAG, "renderParagraph");
		int length = text.length();
		Bidi para = new Bidi();
		try {
			para.setPara(text,
					textDirection != 0 ? Bidi.LEVEL_DEFAULT_RTL
							: Bidi.LEVEL_DEFAULT_LTR,
							null);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		byte paraLevel = (byte)(1 & para.getParaLevel());
		StyleRun styleRun = new StyleRun(length, styleNormal);

		if (styleRuns == null || styleRunCount <= 0) {
			styleRuns = new StyleRun[1];
			styleRunCount = 1;
			styleRuns[0] = styleRun;
		}
		// assume styleRuns[styleRunCount-1].limit>=length

		int width = getTextWidth(text, 0, length, styleRuns, styleRunCount);
		if (width <= lineWidth) {
			// everything fits onto one line

			// prepare rendering a new line from either left or right
			startLine(paraLevel, width);

			renderLine(para, text, 0, length, styleRuns, styleRunCount);
		} else {
			// we need to render several lines
			Bidi line = new Bidi(length, 0);
			int start = 0, limit;
			int styleRunStart = 0, styleRunLimit;

			for (;;) {
				limit = length;
				styleRunLimit = styleRunCount;
				width = getLineBreak(text, new Bounds(start, limit),
						para, styleRuns,
						new Bounds(styleRunStart, styleRunLimit));
				try {
					line = para.setLine(start, limit);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				// prepare rendering a new line
				// from either left or right
				startLine(paraLevel, width);

				if (styleRunStart > 0) {
					int newRunCount = styleRuns.length - styleRunStart;
					StyleRun[] newRuns = new StyleRun[newRunCount];
					System.arraycopy(styleRuns, styleRunStart, newRuns, 0,
							newRunCount);
					renderLine(line, text, start, limit, newRuns,
							styleRunLimit - styleRunStart);
				} else {
					renderLine(line, text, start, limit, styleRuns,
							styleRunLimit - styleRunStart);
				}
				if (limit == length) {
					break;
				}
				start = limit;
				styleRunStart = styleRunLimit - 1;
				if (start >= styleRuns[styleRunStart].limit) {
					++styleRunStart;
				}
			}
		}
	}

	static void render_start()
	{
		renderParagraph("Some Latin text...", Bidi.LTR, null, 0, 80);
		renderParagraph("Some Hebrew text...", Bidi.RTL, null, 0, 60);
	}


}

