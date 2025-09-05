import * as React from "react";
import * as SheetPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

const Sheet = SheetPrimitive.Root;
const SheetTrigger = SheetPrimitive.Trigger;
const SheetClose = SheetPrimitive.Close;

const SheetContent = React.forwardRef(({ className, children, ...props }, ref) => (
  <SheetPrimitive.Portal>
    <SheetPrimitive.Overlay className="fixed inset-0 z-50 bg-black/40 backdrop-blur-sm" />
    <SheetPrimitive.Content
      ref={ref}
      className={cn(
        "fixed right-0 top-0 z-50 h-full w-80 bg-white shadow-xl border-l border-slate-200 focus:outline-none animate-in slide-in-from-right",
        className
      )}
      {...props}
    >
      {children}
      <SheetPrimitive.Close className="absolute right-4 top-4 rounded-md opacity-70 ring-offset-white transition-opacity hover:opacity-100 focus:outline-none">
        <X className="h-5 w-5" />
        <span className="sr-only">Close</span>
      </SheetPrimitive.Close>
    </SheetPrimitive.Content>
  </SheetPrimitive.Portal>
));
SheetContent.displayName = "SheetContent";

const SheetHeader = ({ className, ...props }) => (
  <div className={cn("flex flex-col space-y-2 text-center sm:text-left p-4", className)} {...props} />
);

const SheetTitle = ({ className, ...props }) => (
  <h2 className={cn("text-lg font-semibold text-slate-900", className)} {...props} />
);

export { Sheet, SheetTrigger, SheetContent, SheetHeader, SheetTitle, SheetClose };
