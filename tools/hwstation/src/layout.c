#include "hs.h"

#include <string.h>

int hs_layout_buttons(HsRect *out, int count, int origin_x, int origin_y, int width, int min_btn_w, int btn_h, int gap) {
	int cols;
	int btn_w;
	int i;
	if (out == NULL || count <= 0 || width < min_btn_w || min_btn_w < 1 || btn_h < 1 || gap < 0) {
		return -1;
	}
	cols = width / (min_btn_w + gap);
	if (cols < 1) {
		cols = 1;
	}
	while (cols > 1) {
		btn_w = (width - gap * (cols - 1)) / cols;
		if (btn_w >= min_btn_w) {
			break;
		}
		cols--;
	}
	btn_w = (width - gap * (cols - 1)) / cols;
	if (btn_w < min_btn_w) {
		return -1;
	}
	for (i = 0; i < count; i++) {
		int col = i % cols;
		int row = i / cols;
		out[i].x = origin_x + col * (btn_w + gap);
		out[i].y = origin_y + row * (btn_h + gap);
		out[i].w = btn_w;
		out[i].h = btn_h;
		if (out[i].x + out[i].w > origin_x + width) {
			return -1;
		}
	}
	return (count + cols - 1) / cols;
}

int hs_layout_columns(int *widths, const int *min_widths, int count, int total) {
	int i;
	int sum = 0;
	int extra;
	if (widths == NULL || min_widths == NULL || count <= 0 || total <= 0) {
		return 0;
	}
	for (i = 0; i < count; i++) {
		if (min_widths[i] < 1) {
			return 0;
		}
		sum += min_widths[i];
	}
	if (sum > total) {
		int used = 0;
		for (i = 0; i < count - 1; i++) {
			widths[i] = total * min_widths[i] / sum;
			if (widths[i] < 1) {
				widths[i] = 1;
			}
			used += widths[i];
		}
		widths[count - 1] = total - used;
		if (widths[count - 1] < 1) {
			return 0;
		}
	} else {
		for (i = 0; i < count; i++) {
			widths[i] = min_widths[i];
		}
		extra = total - sum;
		widths[count - 1] += extra;
	}
	sum = 0;
	for (i = 0; i < count; i++) {
		sum += widths[i];
	}
	return sum == total;
}
