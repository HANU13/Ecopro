import * as React from "react";
import { cn } from "@/lib/utils";

function Badge({ className, variant = "default", ...props }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors",
        variant === "default" &&
          "border-transparent bg-indigo-100 text-indigo-800",
        variant === "secondary" &&
          "border-transparent bg-slate-100 text-slate-800",
        variant === "outline" &&
          "text-slate-800 border border-slate-200",
        className
      )}
      {...props}
    />
  );
}

export { Badge };
