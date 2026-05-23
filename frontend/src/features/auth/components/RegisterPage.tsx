import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { FaEnvelope, FaLock, FaStore, FaUser, FaEye, FaEyeSlash } from "react-icons/fa";
import { registerService } from "../services/registerService";

const RegisterPage = () => {
  const [username, setUsername] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!username.trim() || !email.trim() || !password.trim() || !confirmPassword.trim()) {
      toast.error("Vui lòng nhập đầy đủ thông tin.");
      return;
    }

    if (password !== confirmPassword) {
      toast.error("Mật khẩu xác nhận không khớp.");
      return;
    }

    setIsSubmitting(true);
    try {
      await registerService({
        username: username.trim(),
        email: email.trim(),
        password: password.trim(),
      });

      toast.success("Đăng ký thành công! Vui lòng đăng nhập.");
      navigate("/login");
    } catch (error: unknown) {
      const errorMessage =
        typeof error === "object" &&
        error !== null &&
        "response" in error &&
        typeof (error as { response?: { data?: { message?: string } } }).response
          ?.data?.message === "string"
          ? (error as { response: { data: { message: string } } }).response.data.message
          : "Đăng ký thất bại. Email có thể đã tồn tại.";
      toast.error(errorMessage);
      console.error("Register error:", error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="app-shell grid min-h-screen place-items-center px-4 py-8 sm:px-6 lg:px-8">
      <section className="animate-scale-up w-full max-w-md rounded-3xl border border-zinc-200/50 bg-white p-8 shadow-2xl shadow-zinc-200/40 sm:p-10">
        <div className="mb-8">
          <div className="mb-6 flex h-11 w-11 items-center justify-center rounded-xl bg-zinc-950 text-white">
            <FaStore className="h-4 w-4 text-amber-200" />
          </div>
          <div className="badge mb-3 bg-emerald-50 text-emerald-700 border-emerald-100/50">Tạo tài khoản mới</div>
          <h1 className="text-2xl font-extrabold tracking-tight text-zinc-950 sm:text-3xl">Đăng ký</h1>
          <p className="mt-1 text-sm font-medium leading-relaxed text-zinc-500">
            Tạo tài khoản để lưu trữ giỏ hàng, nhận ưu đãi và theo dõi đơn hàng của bạn.
          </p>
        </div>

        <form onSubmit={handleRegister} className="space-y-4">
          <div>
            <label className="label">Tên đăng nhập</label>
            <div className="relative">
              <FaUser className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="field pl-11"
                placeholder="Nhập tên đăng nhập"
                name="username"
                autoComplete="username"
              />
            </div>
          </div>

          <div>
            <label className="label">Email</label>
            <div className="relative">
              <FaEnvelope className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="field pl-11"
                placeholder="Nhập địa chỉ email"
                name="email"
                autoComplete="email"
              />
            </div>
          </div>

          <div>
            <label className="label">Mật khẩu</label>
            <div className="relative">
              <FaLock className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
              <input
                type={showPassword ? "text" : "password"}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="field pl-11 pr-10"
                placeholder="Nhập mật khẩu của bạn"
                name="password"
                autoComplete="new-password"
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 focus:outline-none transition-colors"
                aria-label={showPassword ? "Ẩn mật khẩu" : "Hiển thị mật khẩu"}
              >
                {showPassword ? (
                  <FaEyeSlash className="h-4 w-4" />
                ) : (
                  <FaEye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          <div>
            <label className="label">Xác nhận mật khẩu</label>
            <div className="relative">
              <FaLock className="pointer-events-none absolute left-4 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-zinc-400" />
              <input
                type={showConfirmPassword ? "text" : "password"}
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="field pl-11 pr-10"
                placeholder="Nhập lại mật khẩu để xác nhận"
                name="confirmPassword"
                autoComplete="new-password"
              />
              <button
                type="button"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-zinc-400 hover:text-zinc-600 focus:outline-none transition-colors"
                aria-label={showConfirmPassword ? "Ẩn xác nhận mật khẩu" : "Hiển thị xác nhận mật khẩu"}
              >
                {showConfirmPassword ? (
                  <FaEyeSlash className="h-4 w-4" />
                ) : (
                  <FaEye className="h-4 w-4" />
                )}
              </button>
            </div>
          </div>

          <button type="submit" className="btn-primary w-full mt-6 h-12 rounded-xl bg-zinc-950 hover:bg-zinc-800" disabled={isSubmitting}>
            {isSubmitting ? (
              <span className="h-5 w-5 animate-spin rounded-full border-2 border-white/30 border-t-white" />
            ) : (
              "Đăng ký tài khoản"
            )}
          </button>
        </form>

        <div className="mt-8 text-center text-sm font-semibold text-zinc-500">
          Đã có tài khoản?{" "}
          <Link to="/login" className="font-bold text-zinc-900 underline underline-offset-4 hover:text-zinc-700">
            Đăng nhập ngay
          </Link>
        </div>
      </section>
    </main>
  );
};

export default RegisterPage;
