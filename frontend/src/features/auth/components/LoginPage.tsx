import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { FaLock, FaStore, FaUser, FaCompass, FaShieldAlt, FaBolt, FaEye, FaEyeSlash } from "react-icons/fa";
import { useAuthStore } from "@/store/useAuthStore";
import { z } from "zod";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";

const loginSchema = z.object({
  username: z.string().min(1, "Tên đăng nhập không được để trống"),
  password: z.string().min(1, "Mật khẩu không được để trống"),
});

type LoginValues = z.infer<typeof loginSchema>;

const LoginPage = () => {
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();
  const { user, login } = useAuthStore();

  const form = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      username: "",
      password: "",
    },
  });

  useEffect(() => {
    if (user) {
      if (user.role === "ROLE_ADMIN") {
        navigate("/admin/dashboard", { replace: true });
      } else {
        navigate("/authenticated/products", { replace: true });
      }
    }
  }, [user, navigate]);

  const onSubmit = async (values: LoginValues) => {
    setIsSubmitting(true);
    try {
      const response = await login(values.username.trim(), values.password.trim());
      if (response) {
        toast.success("Đăng nhập thành công!");
        if (response.role === "ROLE_ADMIN") {
          navigate("/admin/dashboard", { replace: true });
        } else {
          navigate("/authenticated/products", { replace: true });
        }
      }
    } catch (error) {
      toast.error("Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
      console.error("Login error:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="app-shell grid min-h-screen place-items-center px-4 py-8 sm:px-6 lg:px-8">
      <section className="animate-scale-up grid w-full max-w-5xl overflow-hidden rounded-3xl border border-zinc-200/50 bg-white shadow-2xl shadow-zinc-200/40 lg:grid-cols-[1.1fr_1fr] min-h-[640px]">
        {/* Tech-Obsidian Left Branding Column */}
        <div className="relative hidden bg-zinc-950 p-12 text-white lg:flex lg:flex-col lg:justify-between overflow-hidden">
          {/* Subtle Ambient Radial Glowing Lights */}
          <div className="absolute -left-12 -top-12 h-64 w-64 rounded-full bg-indigo-500/10 blur-[80px]" />
          <div className="absolute -bottom-20 -right-20 h-80 w-80 rounded-full bg-amber-500/10 blur-[100px]" />

          <div className="relative z-10">
            <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-white/10 text-white backdrop-blur-md border border-white/10">
              <FaStore className="h-4 w-4 text-amber-200" />
            </div>
            <h1 className="mt-8 text-3xl font-extrabold tracking-tight text-white sm:text-4xl">
              Aetheris <span className="text-sm font-normal uppercase tracking-widest text-amber-300/80 block mt-2">Design Concept</span>
            </h1>
            <p className="mt-4 max-w-md text-sm font-medium leading-relaxed text-zinc-400">
              Khám phá thế giới mua sắm cao cấp với các sản phẩm được tuyển chọn kỹ lưỡng, quy trình thanh toán nhanh chóng và bảo mật tuyệt đối.
            </p>
          </div>

          <div className="relative z-10 grid grid-cols-3 gap-4 text-xs font-semibold">
            <div className="rounded-2xl bg-white/[0.03] border border-white/[0.05] p-5 backdrop-blur-sm transition duration-300 hover:bg-white/[0.05]">
              <FaCompass className="h-4 w-4 text-amber-300 mb-3" />
              <p className="text-zinc-200 text-sm font-bold">24/7</p>
              <p className="mt-1 text-[10px] text-zinc-500 uppercase tracking-wider">Hỗ trợ khách</p>
            </div>
            <div className="rounded-2xl bg-white/[0.03] border border-white/[0.05] p-5 backdrop-blur-sm transition duration-300 hover:bg-white/[0.05]">
              <FaShieldAlt className="h-4 w-4 text-indigo-300 mb-3" />
              <p className="text-zinc-200 text-sm font-bold">JWT</p>
              <p className="mt-1 text-[10px] text-zinc-500 uppercase tracking-wider">Mã hóa tối tân</p>
            </div>
            <div className="rounded-2xl bg-white/[0.03] border border-white/[0.05] p-5 backdrop-blur-sm transition duration-300 hover:bg-white/[0.05]">
              <FaBolt className="h-4 w-4 text-amber-300 mb-3" />
              <p className="text-zinc-200 text-sm font-bold">Instant</p>
              <p className="mt-1 text-[10px] text-zinc-500 uppercase tracking-wider">Xử lý đơn hàng</p>
            </div>
          </div>
        </div>

        {/* Elegant Form Column */}
        <div className="flex flex-col justify-center p-8 sm:p-12 lg:p-16">
          <div className="mb-8">
            <div className="mb-6 flex h-11 w-11 items-center justify-center rounded-xl bg-zinc-950 text-white lg:hidden">
              <FaStore className="h-4 w-4 text-amber-200" />
            </div>
            <div className="badge mb-3 bg-zinc-50 text-zinc-600 border-zinc-200/50">Chào mừng trở lại</div>
            <h2 className="text-2xl font-extrabold tracking-tight text-zinc-950 sm:text-3xl">Đăng nhập</h2>
            <p className="mt-1 text-sm font-medium leading-relaxed text-zinc-500">
              Nhập thông tin tài khoản của bạn để bắt đầu trải nghiệm mua sắm.
            </p>
          </div>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Tên đăng nhập</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <FaUser className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400 z-10" />
                        <Input
                          {...field}
                          className="pl-11 h-12 rounded-xl border-zinc-200/60 bg-zinc-50/50"
                          placeholder="Nhập tên đăng nhập của bạn"
                          autoComplete="username"
                        />
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Mật khẩu</FormLabel>
                    <FormControl>
                      <div className="relative">
                        <FaLock className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400 z-10" />
                        <Input
                          {...field}
                          type={showPassword ? "text" : "password"}
                          className="pl-11 pr-10 h-12 rounded-xl border-zinc-200/60 bg-zinc-50/50"
                          placeholder="Nhập mật khẩu"
                          autoComplete="current-password"
                        />
                        <button
                          type="button"
                          onClick={() => setShowPassword(!showPassword)}
                          className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 focus:outline-none transition-colors z-10"
                          aria-label={showPassword ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"}
                        >
                          {showPassword ? (
                            <FaEyeSlash className="h-4 w-4" />
                          ) : (
                            <FaEye className="h-4 w-4" />
                          )}
                        </button>
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              <Button type="submit" className="w-full mt-6 h-12 rounded-xl bg-zinc-950 hover:bg-zinc-800" disabled={isSubmitting}>
                {isSubmitting ? (
                  <span className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                ) : (
                  "Đăng nhập tài khoản"
                )}
              </Button>
            </form>
          </Form>

          <div className="mt-8 text-center text-sm font-semibold text-zinc-500">
            Chưa có tài khoản?{" "}
            <Link to="/register" className="font-bold text-zinc-900 underline underline-offset-4 hover:text-zinc-700">
              Đăng ký ngay
            </Link>
          </div>
        </div>
      </section>
    </main>
  );
};

export default LoginPage;
