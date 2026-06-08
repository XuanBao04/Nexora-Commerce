import React, { useEffect, useRef } from 'react';
import { createPortal } from 'react-dom';
import { FaTimes, FaExclamationTriangle, FaCheckCircle, FaInfoCircle } from 'react-icons/fa';

interface ConfirmModalProps {
  isOpen: boolean;
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
  type?: 'danger' | 'warning' | 'info' | 'success';
}

export const ConfirmModal: React.FC<ConfirmModalProps> = ({
  isOpen,
  title = "Xác nhận",
  message,
  confirmLabel = "Xác nhận",
  cancelLabel = "Hủy bỏ",
  onConfirm,
  onCancel,
  isLoading = false,
  type = 'info'
}) => {
  const confirmButtonRef = useRef<HTMLButtonElement>(null);
  const cancelButtonRef = useRef<HTMLButtonElement>(null);

  // Close on Escape key press
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && isOpen && !isLoading) {
        onCancel();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onCancel, isLoading]);

  // Manage body scroll and focus when modal opens
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden';
      // Focus on the cancel/dismiss button by default for safety (fail-safe UX)
      setTimeout(() => cancelButtonRef.current?.focus(), 50);
    } else {
      document.body.style.overflow = '';
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleBackdropClick = (e: React.MouseEvent) => {
    if (e.target === e.currentTarget && !isLoading) {
      onCancel();
    }
  };

  // Get icon and colors based on type
  const getIconAndColors = () => {
    switch (type) {
      case 'danger':
        return {
          icon: <FaExclamationTriangle className="w-6 h-6 text-rose-500" />,
          iconBg: 'bg-rose-50 border border-rose-100',
          confirmBtn: 'bg-rose-600 hover:bg-rose-700 text-white focus:ring-rose-500/20',
        };
      case 'warning':
        return {
          icon: <FaExclamationTriangle className="w-6 h-6 text-amber-500" />,
          iconBg: 'bg-amber-50 border border-amber-100',
          confirmBtn: 'bg-amber-500 hover:bg-amber-600 text-white focus:ring-amber-500/20',
        };
      case 'success':
        return {
          icon: <FaCheckCircle className="w-6 h-6 text-emerald-500" />,
          iconBg: 'bg-emerald-50 border border-emerald-100',
          confirmBtn: 'bg-emerald-600 hover:bg-emerald-700 text-white focus:ring-emerald-500/20',
        };
      case 'info':
      default:
        return {
          icon: <FaInfoCircle className="w-6 h-6 text-blue-500" />,
          iconBg: 'bg-blue-50 border border-blue-100',
          confirmBtn: 'bg-zinc-950 hover:bg-zinc-800 text-white focus:ring-zinc-950/20',
        };
    }
  };

  const colors = getIconAndColors();

  return createPortal(
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-zinc-950/60 backdrop-blur-md p-4 animate-fade-in"
      onClick={handleBackdropClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-modal-title"
    >
      <div 
        className="relative w-full max-w-md bg-white rounded-3xl shadow-2xl border border-zinc-200/80 overflow-hidden animate-scale-up p-6 md:p-8 space-y-6"
      >
        {/* Close Button */}
        {!isLoading && (
          <button
            onClick={onCancel}
            ref={cancelButtonRef}
            className="absolute top-4 right-4 z-10 w-9 h-9 flex items-center justify-center bg-zinc-50 hover:bg-zinc-100 text-zinc-400 hover:text-zinc-700 rounded-full transition shadow-sm border border-zinc-100/80"
            title="Đóng"
            aria-label="Đóng hộp thoại"
          >
            <FaTimes className="w-3.5 h-3.5" />
          </button>
        )}

        {/* Modal Content */}
        <div className="flex flex-col items-center text-center space-y-4 pt-2">
          {/* Visual Indicator Icon */}
          <div className={`w-14 h-14 rounded-2xl flex items-center justify-center ${colors.iconBg} shadow-sm`}>
            {colors.icon}
          </div>

          <div className="space-y-2">
            <h2 
              id="confirm-modal-title" 
              className="text-lg font-extrabold text-zinc-950 tracking-tight"
            >
              {title}
            </h2>
            <p className="text-xs md:text-sm font-semibold text-zinc-500 leading-relaxed px-2">
              {message}
            </p>
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex flex-col sm:flex-row gap-2 pt-2">
          <button
            onClick={onCancel}
            disabled={isLoading}
            className="w-full sm:order-1 h-11 rounded-xl text-xs font-bold uppercase tracking-wider bg-zinc-50 hover:bg-zinc-100 text-zinc-700 border border-zinc-200/50 transition-all duration-250 active:scale-[0.98] disabled:opacity-50 disabled:pointer-events-none"
          >
            {cancelLabel}
          </button>
          
          <button
            onClick={onConfirm}
            ref={confirmButtonRef}
            disabled={isLoading}
            className={`w-full sm:order-2 h-11 rounded-xl text-xs font-bold uppercase tracking-wider flex items-center justify-center gap-2 transition-all duration-250 active:scale-[0.98] focus:outline-none focus:ring-4 disabled:opacity-50 disabled:pointer-events-none ${colors.confirmBtn}`}
          >
            {isLoading ? (
              <span className="w-4 h-4 rounded-full border-2 border-white/30 border-t-white animate-spin" />
            ) : (
              <span>{confirmLabel}</span>
            )}
          </button>
        </div>
      </div>
    </div>,
    document.body
  );
};
