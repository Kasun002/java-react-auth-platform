// ── Pagination ────────────────────────────────────────────────────────────────

interface PaginationProps {
  page: number;
  totalPages: number;
  totalElements: number;
  onPageChange: (p: number) => void;
}

const Pagination = ({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: PaginationProps) => {

  const pageNumbers = Array.from({ length: totalPages }, (_, i) => i);
  
  const visible = pageNumbers.reduce<(number | "…")[]>((acc, i) => {
    if (i === 0 || i === totalPages - 1 || Math.abs(i - page) <= 1) {
      if (acc.length > 0 && typeof acc[acc.length - 1] === "number") {
        const prev = acc[acc.length - 1] as number;
        if (i > prev + 1) acc.push("…");
      }
      acc.push(i);
    }
    return acc;
  }, []);

  return (
    <div className="flex items-center justify-between border-t border-gray-100 dark:border-gray-800 px-6 py-3">
      <span className="text-xs text-gray-500 dark:text-gray-400 tabular-nums">
        Page {page + 1} of {totalPages} &middot; {totalElements} users total
      </span>
      <div className="flex items-center gap-1">
        <button
          onClick={() => onPageChange(Math.max(0, page - 1))}
          disabled={page === 0}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5 transition-colors"
        >
          ← Prev
        </button>
        {visible.map((item, idx) =>
          item === "…" ? (
            <span key={`el-${idx+1}`} className="px-1.5 text-xs text-gray-400">
              …
            </span>
          ) : (
            <button
              key={item}
              onClick={() => onPageChange(item)}
              className={`min-w-[28px] rounded-lg border px-2 py-1.5 text-xs font-medium transition-colors ${
                page === item
                  ? "border-brand-500 bg-brand-500 text-white"
                  : "border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5"
              }`}
            >
              {(item) + 1}
            </button>
          )
        )}
        <button
          onClick={() => onPageChange(Math.min(totalPages - 1, page + 1))}
          disabled={page >= totalPages - 1}
          className="rounded-lg border border-gray-200 px-3 py-1.5 text-xs font-medium text-gray-600 hover:bg-gray-50 disabled:opacity-40 disabled:cursor-not-allowed dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5 transition-colors"
        >
          Next →
        </button>
      </div>
    </div>
  );
};
export default Pagination;
