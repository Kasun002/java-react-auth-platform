interface Props {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  /** Controls the outer wrapper — use Tailwind width classes e.g. "w-72", "w-80" (default: "w-72") */
  className?: string;
}

export default function SearchInput({
  value,
  onChange,
  placeholder = "Search…",
  className = "w-72",
}: Readonly<Props>) {
  return (
    <div className={`relative ${className}`}>
      <svg
        className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      >
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.35-4.35" />
      </svg>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="h-10 w-full rounded-lg border border-gray-200 bg-white pl-9 pr-3 text-sm text-gray-800 placeholder-gray-400 focus:border-brand-500 focus:outline-none dark:border-gray-700 dark:bg-gray-800 dark:text-white/90"
      />
    </div>
  );
}
