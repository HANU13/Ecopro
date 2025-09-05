import React, { useMemo, useState } from "react";
import { Canvas } from "@react-three/fiber";
import { Float, PresentationControls, Environment, ContactShadows, Html } from "@react-three/drei";
import { motion, AnimatePresence } from "framer-motion";
import { ShoppingCart, Search, Minus, Plus, X, Sparkles } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet";
import { Badge } from "@/components/ui/badge";

/**
 * 3D Animated Ecommerce Website — Single-file React starter
 * - Tech: React + Tailwind + Framer Motion + @react-three/fiber + drei + shadcn/ui
 * - Features: 3D hero, product grid, live cart, search/filter, micro-interactions
 * - Replace placeholder 3D shapes with your own GLTF/GLB model URLs inside <ProductModel />
 */

const PRODUCTS = [
  {
    id: "neo-shoe",
    name: "Neo Runner X1",
    price: 129.0,
    tag: "New",
    colors: ["#0ea5e9", "#6366f1", "#ef4444"],
    description: "Featherweight runners with responsive foam and air mesh upper.",
  },
  {
    id: "aurora-headset",
    name: "Aurora Headset",
    price: 199.0,
    tag: "Hot",
    colors: ["#22c55e", "#f59e0b", "#3b82f6"],
    description: "Spatial audio, hybrid ANC, and 40h battery for marathon sessions.",
  },
  {
    id: "quantum-watch",
    name: "Quantum Watch S",
    price: 249.0,
    tag: "Sale",
    colors: ["#ef4444", "#10b981", "#6b7280"],
    description: "AMOLED display, ECG, and multi-sport GPS in a sleek shell.",
  },
  {
    id: "nova-pack",
    name: "Nova Daypack",
    price: 89.0,
    tag: "Eco",
    colors: ["#6ee7b7", "#93c5fd", "#fde68a"],
    description: "Recycled shell, modular pockets, rain cover included.",
  },
];

function currency(n) {
  return new Intl.NumberFormat(undefined, { style: "currency", currency: "USD" }).format(n);
}

function useCart() {
  const [items, setItems] = useState({}); // id -> qty
  const add = (id) => setItems((m) => ({ ...m, [id]: (m[id] || 0) + 1 }));
  const inc = (id) => setItems((m) => ({ ...m, [id]: (m[id] || 0) + 1 }));
  const dec = (id) => setItems((m) => ({ ...m, [id]: Math.max(0, (m[id] || 0) - 1) }));
  const remove = (id) => setItems((m) => { const c = { ...m }; delete c[id]; return c; });
  const clear = () => setItems({});
  const count = Object.values(items).reduce((a, b) => a + b, 0);
  const total = Object.entries(items).reduce((sum, [id, qty]) => {
    const p = PRODUCTS.find((x) => x.id === id);
    return sum + (p ? p.price * qty : 0);
  }, 0);
  return { items, add, inc, dec, remove, clear, count, total };
}

function ProductModel({ color = "#6366f1" }) {
  // Replace placeholder with your GLTF model: 
  // const gltf = useGLTF("/models/yourModel.glb")
  return (
    <Float speed={2} rotationIntensity={1} floatIntensity={1}>
      <PresentationControls global polar={[0, 0]} azimuth={[-Math.PI / 8, Math.PI / 8]}>
        <mesh castShadow receiveShadow>
          <torusKnotGeometry args={[0.6, 0.22, 256, 32]} />
          <meshPhysicalMaterial
            roughness={0.15}
            metalness={0.7}
            color={color}
            envMapIntensity={1.2}
            clearcoat={1}
            clearcoatRoughness={0.1}
          />
        </mesh>
      </PresentationControls>
    </Float>
  );
}

function Hero3D() {
  return (
    <div className="relative w-full h-[70vh] md:h-[80vh] rounded-2xl overflow-hidden bg-gradient-to-br from-slate-900 via-indigo-900 to-violet-800">
      <Canvas shadows camera={{ position: [0, 0, 3.2], fov: 50 }}>
        <ambientLight intensity={0.4} />
        <directionalLight castShadow position={[3, 5, 2]} intensity={1.4} />
        <ProductModel />
        <Environment preset="city" />
        <ContactShadows position={[0, -1, 0]} blur={2.4} opacity={0.4} scale={10} />
        <Html center>
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
            className="text-center"
          >
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur text-white text-xs mb-4">
              <Sparkles className="w-3.5 h-3.5" /> Live 3D Preview
            </div>
            <h1 className="text-4xl md:text-6xl font-extrabold tracking-tight text-white">
              Elevate Your <span className="text-indigo-300">Store</span>
            </h1>
            <p className="mt-3 md:mt-4 text-sm md:text-base text-indigo-100 max-w-lg">
              Immersive products, silky animations, and a blazing-fast cart — all in one modern template.
            </p>
          </motion.div>
        </Html>
      </Canvas>
      <div className="pointer-events-none absolute inset-0 ring-1 ring-white/10 rounded-2xl" />
    </div>
  );
}

export default function Ecommerce3DApp() {
  const cart = useCart();
  const [query, setQuery] = useState("");
  const [activeColor, setActiveColor] = useState({}); // id -> color

  const filtered = useMemo(() => {
    const q = query.toLowerCase();
    return PRODUCTS.filter((p) => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q));
  }, [query]);

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      {/* NAVBAR */}
      <header className="sticky top-0 z-50 backdrop-blur bg-white/70 border-b border-slate-200">
        <div className="max-w-6xl mx-auto px-4 py-3 flex items-center gap-3">
          <div className="font-black text-xl tracking-tight">neo<span className="text-indigo-600">store</span></div>

          <div className="ml-auto hidden md:flex items-center gap-3">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
              <Input
                placeholder="Search products"
                className="pl-10 w-72"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>

            <Sheet>
              <SheetTrigger asChild>
                <Button variant="default" className="relative">
                  <ShoppingCart className="w-4 h-4 mr-2" /> Cart
                  {cart.count > 0 && (
                    <span className="absolute -top-2 -right-2 w-6 h-6 rounded-full bg-indigo-600 text-white text-xs grid place-items-center">
                      {cart.count}
                    </span>
                  )}
                </Button>
              </SheetTrigger>
              <SheetContent className="w-full sm:max-w-md">
                <SheetHeader>
                  <SheetTitle>Your Cart</SheetTitle>
                </SheetHeader>
                <div className="mt-4 space-y-4">
                  {Object.entries(cart.items).length === 0 && (
                    <p className="text-sm text-slate-500">Your cart is empty.</p>
                  )}

                  {Object.entries(cart.items).map(([id, qty]) => {
                    const p = PRODUCTS.find((x) => x.id === id)!;
                    return (
                      <Card key={id}>
                        <CardContent className="p-4 flex items-center gap-4">
                          <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-indigo-200 to-violet-200" />
                          <div className="flex-1">
                            <div className="flex items-center gap-2">
                              <span className="font-semibold">{p.name}</span>
                              <Badge variant="secondary">{p.tag}</Badge>
                            </div>
                            <div className="text-xs text-slate-500">{currency(p.price)}</div>
                          </div>
                          <div className="flex items-center gap-2">
                            <Button size="icon" variant="outline" onClick={() => cart.dec(id)}>
                              <Minus className="w-4 h-4" />
                            </Button>
                            <span className="w-6 text-center">{qty}</span>
                            <Button size="icon" onClick={() => cart.inc(id)}>
                              <Plus className="w-4 h-4" />
                            </Button>
                          </div>
                          <Button size="icon" variant="ghost" onClick={() => cart.remove(id)}>
                            <X className="w-4 h-4" />
                          </Button>
                        </CardContent>
                      </Card>
                    );
                  })}
                  <div className="flex items-center justify-between pt-2 border-t">
                    <span className="text-sm font-medium">Total</span>
                    <span className="text-base font-semibold">{currency(cart.total)}</span>
                  </div>
                  <Button className="w-full">Checkout</Button>
                </div>
              </SheetContent>
            </Sheet>
          </div>
        </div>
      </header>

      {/* HERO */}
      <main className="max-w-6xl mx-auto px-4 py-8 md:py-12">
        <Hero3D />

        {/* MOBILE SEARCH + CART */}
        <div className="mt-6 flex md:hidden items-center gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400" />
            <Input
              placeholder="Search products"
              className="pl-10"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
            />
          </div>
          <Sheet>
            <SheetTrigger asChild>
              <Button variant="outline" className="relative">
                <ShoppingCart className="w-4 h-4" />
                {cart.count > 0 && (
                  <span className="absolute -top-2 -right-2 w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] grid place-items-center">
                    {cart.count}
                  </span>
                )}
              </Button>
            </SheetTrigger>
            <SheetContent className="w-full sm:max-w-md">
              <SheetHeader>
                <SheetTitle>Your Cart</SheetTitle>
              </SheetHeader>
              <div className="mt-4 space-y-4">
                {Object.entries(cart.items).length === 0 && (
                  <p className="text-sm text-slate-500">Your cart is empty.</p>
                )}
                {Object.entries(cart.items).map(([id, qty]) => {
                  const p = PRODUCTS.find((x) => x.id === id)!;
                  return (
                    <Card key={id}>
                      <CardContent className="p-4 flex items-center gap-4">
                        <div className="w-14 h-14 rounded-xl bg-gradient-to-br from-indigo-200 to-violet-200" />
                        <div className="flex-1">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold">{p.name}</span>
                            <Badge variant="secondary">{p.tag}</Badge>
                          </div>
                          <div className="text-xs text-slate-500">{currency(p.price)}</div>
                        </div>
                        <div className="flex items-center gap-2">
                          <Button size="icon" variant="outline" onClick={() => cart.dec(id)}>
                            <Minus className="w-4 h-4" />
                          </Button>
                          <span className="w-6 text-center">{qty}</span>
                          <Button size="icon" onClick={() => cart.inc(id)}>
                            <Plus className="w-4 h-4" />
                          </Button>
                        </div>
                        <Button size="icon" variant="ghost" onClick={() => cart.remove(id)}>
                          <X className="w-4 h-4" />
                        </Button>
                      </CardContent>
                    </Card>
                  );
                })}
                <div className="flex items-center justify-between pt-2 border-t">
                  <span className="text-sm font-medium">Total</span>
                  <span className="text-base font-semibold">{currency(cart.total)}</span>
                </div>
                <Button className="w-full">Checkout</Button>
              </div>
            </SheetContent>
          </Sheet>
        </div>

        {/* GRID */}
        <section className="mt-10 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          <AnimatePresence>
            {filtered.map((p) => (
              <motion.div
                key={p.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: -20 }}
                transition={{ duration: 0.3 }}
              >
                <Card className="rounded-2xl shadow-sm hover:shadow-md transition">
                  <CardHeader>
                    <div className="flex items-center justify-between">
                      <CardTitle className="text-lg">{p.name}</CardTitle>
                      <Badge variant="outline">{p.tag}</Badge>
                    </div>
                  </CardHeader>
                  <CardContent>
                    <div className="relative h-52 rounded-xl overflow-hidden bg-gradient-to-br from-indigo-50 to-violet-50">
                      <Canvas camera={{ position: [0, 0, 3.2], fov: 50 }}>
                        <ambientLight intensity={0.5} />
                        <directionalLight position={[2, 3, 4]} intensity={1.1} />
                        <ProductModel color={activeColor[p.id] || p.colors[0]} />
                        <Environment preset="sunset" />
                      </Canvas>
                    </div>

                    <p className="mt-3 text-sm text-slate-600 min-h-[40px]">{p.description}</p>
                    <div className="mt-4 flex items-center justify-between">
                      <div className="font-semibold">{currency(p.price)}</div>
                      <div className="flex items-center gap-2">
                        {p.colors.map((c) => (
                          <button
                            key={c}
                            onClick={() => setActiveColor((m) => ({ ...m, [p.id]: c }))}
                            className="w-6 h-6 rounded-full ring-2 ring-white outline outline-1 outline-slate-200"
                            style={{ background: c }}
                            aria-label={`Select color ${c}`}
                          />
                        ))}
                      </div>
                    </div>
                    <div className="mt-4 flex items-center gap-3">
                      <Button className="flex-1" onClick={() => cart.add(p.id)}>
                        Add to cart
                      </Button>
                      <Button variant="outline" className="flex-1">Buy now</Button>
                    </div>
                  </CardContent>
                </Card>
              </motion.div>
            ))}
          </AnimatePresence>
        </section>

        {/* CTA */}
        <section className="mt-14 md:mt-20">
          <div className="relative overflow-hidden rounded-2xl p-8 md:p-12 bg-gradient-to-r from-indigo-600 to-violet-600 text-white">
            <h2 className="text-2xl md:text-3xl font-extrabold">Ready to make it yours?</h2>
            <p className="mt-2 text-indigo-100 max-w-2xl">
              Swap in your 3D models, connect your backend (Stripe/Shopify/Supabase), and launch a store that feels alive.
            </p>
            <div className="mt-5 flex flex-wrap gap-3">
              <Button variant="secondary">View Docs</Button>
              <Button variant="outline" className="text-white border-white/50">Contact Sales</Button>
            </div>
            <div className="absolute -right-10 -bottom-10 w-64 h-64 bg-white/10 rounded-full blur-3xl" />
          </div>
        </section>

        {/* FOOTER */}
        <footer className="py-10 text-center text-sm text-slate-500">
          © {new Date().getFullYear()} NeoStore. Built with ♥ for modern commerce.
        </footer>
      </main>
    </div>
  );
}
