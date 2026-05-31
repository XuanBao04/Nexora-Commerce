import { useEffect, useState } from "react";
import { useSearchParams, useNavigate } from "react-router-dom";
import { FaCheckCircle, FaTimesCircle, FaArrowRight } from "react-icons/fa";

import { toast } from "react-toastify";

const PaymentResult = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState<'SUCCESS' | 'FAILED' | 'PENDING'>('PENDING');

  useEffect(() => {
    // Check parameters from VNPAY
    const vnp_ResponseCode = searchParams.get('vnp_ResponseCode');
    
    // VNPAY Success Code is '00'
    if (vnp_ResponseCode !== null) {
      if (vnp_ResponseCode === '00') {
        setStatus('SUCCESS');
        toast.success("Thanh toán thành công!");
      } else {
        setStatus('FAILED');
        toast.error("Thanh toán thất bại. Vui lòng thử lại.");
      }
      return;
    }

    // If no clear parameters, maybe it failed or it was just a manual visit
    setStatus('FAILED');
  }, [searchParams]);

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
