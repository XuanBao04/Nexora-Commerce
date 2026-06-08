import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { FaCheckCircle, FaTimesCircle, FaArrowRight } from "react-icons/fa";
import { QRCodeSVG } from "qrcode.react";

import { toast } from "react-toastify";
import { API_CONFIG } from "@/utils/constants";
import { orderService } from "@/features/orders/services/orderService";

const PENDING_VNPAY_PAYMENT_KEY = "nexora_pending_vnpay_payment";

type PendingVnpayPayment = {
  orderId: string;
  paymentUrl: string;
  totalPrice: number;
  createdAt: string;
};

const loadPendingPayment = (): PendingVnpayPayment | null => {
  const rawValue = sessionStorage.getItem(PENDING_VNPAY_PAYMENT_KEY);

  if (!rawValue) {
    return null;
  }

  try {
    return JSON.parse(rawValue) as PendingVnpayPayment;
  } catch {
    sessionStorage.removeItem(PENDING_VNPAY_PAYMENT_KEY);
    return null;
  }
};

const clearPendingPayment = () => {
  sessionStorage.removeItem(PENDING_VNPAY_PAYMENT_KEY);
};

const buildVnpayReturnUrl = (queryString: string) => {
  const baseUrl = API_CONFIG.BASE_URL.replace(/\/$/, "");
  const returnPath = `${baseUrl}/v1/payments/vnpay/return`;

  if (returnPath.startsWith("http")) {
    return `${returnPath}?${queryString}`;
  }

  return `${window.location.origin}${returnPath}?${queryString}`;
};

const PaymentResult = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'SUCCESS' | 'FAILED' | 'PENDING'>('PENDING');
  const [pendingPayment, setPendingPayment] = useState<PendingVnpayPayment | null>(null);
  const [isRepaying, setIsRepaying] = useState(false);

  useEffect(() => {
    const nexoraResult = searchParams.get('nexora_Result');
    // Kiểm tra tham số từ VNPAY
    const vnp_ResponseCode = searchParams.get('vnp_ResponseCode');
    const pending = loadPendingPayment();

    if (nexoraResult !== null) {
      if (pending) {
        setPendingPayment(pending);
      }
      clearPendingPayment();
      if (nexoraResult === 'SUCCESS') {
        setStatus('SUCCESS');
        toast.success("Thanh toán thành công!");
      } else {
        setStatus('FAILED');
        toast.error("Thanh toán thất bại. Vui lòng thử lại.");
      }
      return;
    }

    if (pending) {
      setPendingPayment(pending);
      setStatus('PENDING');
      return;
    }
    
    // '00' là mã giao dịch thành công của VNPAY
    if (vnp_ResponseCode !== null) {
      setStatus('PENDING');
      window.location.replace(buildVnpayReturnUrl(searchParams.toString()));
      return;
    }

    // Mặc định thất bại nếu không có tham số hợp lệ
    setStatus('FAILED');
  }, [searchParams]);

  const handleRepay = async () => {
    if (!pendingPayment) return;
    setIsRepaying(true);
    try {
      const response = await orderService.repayPayment(pendingPayment.orderId);
      if (response.paymentUrl) {
        // Lưu thông tin thanh toán mới để quét lại khi return về
        sessionStorage.setItem(PENDING_VNPAY_PAYMENT_KEY, JSON.stringify({
          orderId: response.id,
          paymentUrl: response.paymentUrl,
          totalPrice: response.totalPrice,
          createdAt: response.createdAt
        }));
        window.location.href = response.paymentUrl;
      } else {
        toast.error("Không thể tạo liên kết thanh toán mới.");
      }
    } catch (err: Error | unknown) {
      const errorMessage = err instanceof Error ? err.message : "Thao tác thanh toán lại thất bại.";
      toast.error(errorMessage);
    } finally {
      setIsRepaying(false);
    }
  };

  const pendingAmountLabel = pendingPayment
    ? new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(pendingPayment.totalPrice)
    : null;

  return (
    <div className="min-h-screen flex items-center justify-center bg-zinc-50 p-6 animate-fade-in">
      <div className="surface p-8 max-w-md w-full text-center space-y-6">
        {status === 'SUCCESS' ? (
          <>
            <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-emerald-100">
              <FaCheckCircle className="h-12 w-12 text-emerald-600 animate-bounce" />
            </div>
            <h1 className="text-2xl font-black text-zinc-900 tracking-tight">Thanh toán thành công!</h1>
            <p className="text-sm font-medium text-zinc-500">
              Đơn hàng của bạn đã được ghi nhận và thanh toán hoàn tất. Chúng tôi sẽ sớm xử lý và giao hàng cho bạn.
            </p>
          </>
        ) : status === 'FAILED' ? (
          <>
            <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-rose-100">
              <FaTimesCircle className="h-12 w-12 text-rose-600 animate-pulse" />
            </div>
            <h1 className="text-2xl font-black text-zinc-900 tracking-tight">Thanh toán thất bại</h1>
            <p className="text-sm font-medium text-zinc-500">
              Giao dịch của bạn đã bị hủy hoặc xảy ra lỗi trong quá trình thanh toán. Vui lòng thử lại.
            </p>

            {pendingPayment && (
              <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4 space-y-3 text-left">
                <div className="flex items-center justify-between gap-4 text-xs font-bold uppercase tracking-widest text-zinc-400">
                  <span>Mã đơn hàng</span>
                  <span>{pendingPayment.orderId}</span>
                </div>
                <div className="flex items-center justify-between gap-4 text-sm font-extrabold text-zinc-900">
                  <span>Tổng thanh toán</span>
                  <span>{pendingAmountLabel}</span>
                </div>
                <button
                  onClick={handleRepay}
                  disabled={isRepaying}
                  className="btn-primary w-full h-10 uppercase tracking-widest text-[10px] font-bold mt-2"
                >
                  {isRepaying ? "Đang tạo liên kết..." : "Thử thanh toán lại"}
                </button>
              </div>
            )}
          </>
        ) : pendingPayment ? (
          <>
            <div className="space-y-3">
              <div className="mx-auto flex h-24 w-24 items-center justify-center rounded-full bg-indigo-100">
                <QRCodeSVG value={pendingPayment.paymentUrl} size={88} bgColor="transparent" fgColor="#4f46e5" />
              </div>
              <div className="inline-flex items-center gap-1.5 rounded-full border border-indigo-200/60 bg-indigo-50/80 px-3 py-1 text-[10px] font-black uppercase tracking-widest text-indigo-700">
                VNPAY QR
              </div>
            </div>
            <h1 className="text-2xl font-black text-zinc-900 tracking-tight">Quét QR để thanh toán</h1>
            <p className="text-sm font-medium text-zinc-500">
              Dùng ứng dụng VNPAY hoặc app ngân hàng hỗ trợ QR để quét mã và hoàn tất thanh toán test.
            </p>

            <div className="rounded-2xl border border-zinc-200 bg-zinc-50 p-4 text-left space-y-2">
              <div className="flex items-center justify-between gap-4 text-xs font-bold uppercase tracking-widest text-zinc-400">
                <span>Mã đơn hàng</span>
                <span>{pendingPayment.orderId}</span>
              </div>
              <div className="flex items-center justify-between gap-4 text-sm font-extrabold text-zinc-900">
                <span>Tổng thanh toán</span>
                <span>{pendingAmountLabel}</span>
              </div>
            </div>

            <button
              onClick={() => window.open(pendingPayment.paymentUrl, "_blank", "noopener,noreferrer")}
              className="btn-primary w-full h-12 uppercase tracking-widest text-xs font-bold"
            >
              Mở cổng thanh toán
            </button>
          </>
        ) : (
          <div className="py-12">
            <div className="mx-auto h-12 w-12 animate-spin rounded-full border-4 border-indigo-100 border-t-indigo-600" />
            <p className="mt-4 text-sm font-bold text-zinc-500">Đang kiểm tra kết quả...</p>
          </div>
        )}

        <div className="pt-4 space-y-3">
          <button
            onClick={() => navigate('/authenticated/orders')}
            className="btn-primary w-full h-12 uppercase tracking-widest text-xs font-bold"
          >
            Xem đơn hàng
          </button>
          
          <button
            onClick={() => navigate('/authenticated/products')}
            className="btn-secondary w-full h-12 uppercase tracking-widest text-xs font-bold"
          >
            Tiếp tục mua sắm
            <FaArrowRight className="ml-2 h-3 w-3" />
          </button>
        </div>
      </div>
    </div>
  );
};

export default PaymentResult;
