export { apiError } from "../../utils/api";
export {
  inputCls,
  cancelBtnCls,
  saveBtnCls,
  errorBannerCls,
} from "../../utils/styles";

import { GROUP_TYPE_BADGE_COLOR } from "../../types/ui";
import type { BadgeColor } from "../../types/ui";

export function typeColor(type: string): BadgeColor {
  return GROUP_TYPE_BADGE_COLOR[type.toUpperCase()] ?? "light";
}
