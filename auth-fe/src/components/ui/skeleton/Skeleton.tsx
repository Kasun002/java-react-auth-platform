import { memo } from "react";

function Skeleton({ className }: Readonly<{ className?: string }>) {
  return (
    <div className={`animate-pulse rounded bg-gray-100 dark:bg-gray-800 ${className ?? ""}`} />
  );
}

export default memo(Skeleton);
