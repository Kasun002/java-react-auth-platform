import { memo } from "react";

interface Props {
  page: number;
  totalPages: number;
  onPrev: () => void;
  onNext: () => void;
}

const btnCls =
  "rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5";

function Pagination({ page, totalPages, onPrev, onNext }: Readonly<Props>) {
  return (
    <div className="flex items-center justify-between border-t border-gray-100 px-6 py-3 dark:border-gray-800">
      <span className="text-xs text-gray-400 dark:text-gray-500 tabular-nums">
        Page {page + 1} of {totalPages}
      </span>
      <div className="flex items-center gap-2">
        <button onClick={onPrev} disabled={page === 0} className={btnCls}>
          Previous
        </button>
        <button
          onClick={onNext}
          disabled={page >= totalPages - 1}
          className={btnCls}
        >
          Next
        </button>
      </div>
    </div>
  );
}

export default memo(Pagination);
