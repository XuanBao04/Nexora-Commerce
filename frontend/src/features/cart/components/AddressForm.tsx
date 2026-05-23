import { useState } from "react";
import { FaMapMarkerAlt, FaPhoneAlt } from "react-icons/fa";
import { ShippingAddress } from "@/features/orders/types/order";

interface AddressFormProps {
  onAddressChange: (address: ShippingAddress) => void;
  errors?: Partial<ShippingAddress>;
  setErrors?: React.Dispatch<React.SetStateAction<Partial<ShippingAddress>>>;
}

const fieldClass = (hasError?: string) =>
  `field ${hasError ? "border-rose-400 focus:border-rose-500 focus:ring-rose-500/10" : ""}`;

export default function AddressForm({
  onAddressChange,
  errors: propsErrors,
  setErrors: propsSetErrors,
}: AddressFormProps) {
  const [address, setAddress] = useState<ShippingAddress>({
    shippingAddress: "",
    city: "",
    district: "",
    ward: "",
    postalCode: "",
    phoneNumber: "",
  });

  const [localErrors, setLocalErrors] = useState<Partial<ShippingAddress>>({});
  const errors = propsErrors || localErrors;
  const setErrors = propsSetErrors || setLocalErrors;

  const validateField = (name: string, value: string): string | undefined => {
    const trimmed = value.trim();
    if (name === "shippingAddress") {
      if (!trimmed) return "Vui lòng nhập địa chỉ giao hàng";
      if (trimmed.length < 5 || trimmed.length > 255) {
        return "Địa chỉ giao hàng phải từ 5 đến 255 ký tự";
      }
    }
    if (name === "city") {
      if (!trimmed) return "Vui lòng nhập Tỉnh / Thành phố";
      if (trimmed.length < 2 || trimmed.length > 100) {
        return "Tỉnh / Thành phố phải từ 2 đến 100 ký tự";
      }
    }
    if (name === "district") {
      if (!trimmed) return "Vui lòng nhập Quận / Huyện";
      if (trimmed.length < 2 || trimmed.length > 100) {
        return "Quận / Huyện phải từ 2 đến 100 ký tự";
      }
    }
    if (name === "ward") {
      if (!trimmed) return "Vui lòng nhập Phường / Xã";
      if (trimmed.length < 2 || trimmed.length > 100) {
        return "Phường / Xã phải từ 2 đến 100 ký tự";
      }
    }
    if (name === "phoneNumber") {
      if (!trimmed) {
        return "Vui lòng nhập số điện thoại";
      }
      if (!/^0\d{9}$/.test(trimmed)) {
        return "Số điện thoại không hợp lệ (phải gồm 10 chữ số và bắt đầu bằng số 0)";
      }
    }
    return undefined;
  };

  const handleBlur = (e: React.FocusEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    const error = validateField(name, value);
    setErrors((prev) => ({
      ...prev,
      [name]: error,
    }));
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    let processedValue = value;

    if (name === "phoneNumber") {
      processedValue = value.replace(/\D/g, "").slice(0, 10);
    }

    const updatedAddress = { ...address, [name]: processedValue };
    setAddress(updatedAddress);
    onAddressChange(updatedAddress);

    if (errors[name as keyof ShippingAddress]) {
      const error = validateField(name, processedValue);
      setErrors((prev) => ({
        ...prev,
        [name]: error,
      }));
    }
  };

  return (
    <section className="surface p-6 sm:p-8">
      <div className="mb-6 flex items-start gap-4 border-b border-zinc-100 pb-5">
        <div className="flex h-11 w-11 items-center justify-center rounded-xl bg-zinc-950 text-white shadow-md shadow-zinc-900/10">
          <FaMapMarkerAlt className="h-4 w-4 text-amber-200" />
        </div>
        <div>
          <h2 className="text-base font-extrabold text-zinc-950">
            Thông tin giao hàng
          </h2>
          <p className="mt-1 text-xs font-semibold text-zinc-400">
            Vui lòng điền thông tin chính xác để đơn hàng được vận chuyển tốt nhất.
          </p>
        </div>
      </div>

      <div className="space-y-5">
        <div>
          <label htmlFor="shippingAddress" className="label">
            Địa chỉ giao hàng <span className="text-rose-500">*</span>
          </label>
          <input
            id="shippingAddress"
            type="text"
            name="shippingAddress"
            value={address.shippingAddress}
            onChange={handleChange}
            onBlur={handleBlur}
            placeholder="Ví dụ: 123 Đường Nguyễn Huệ, Lầu 3"
            className={fieldClass(errors.shippingAddress)}
            data-testid="shipping-address-input"
          />
          {errors.shippingAddress && (
            <p className="mt-1.5 text-xs font-semibold text-rose-600">
              {errors.shippingAddress}
            </p>
          )}
        </div>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
          <div>
            <label htmlFor="city" className="label">
              Thành phố/Tỉnh <span className="text-rose-500">*</span>
            </label>
            <input
              id="city"
              type="text"
              name="city"
              value={address.city}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Ví dụ: Thành phố Hồ Chí Minh"
              className={fieldClass(errors.city)}
              data-testid="city-input"
            />
            {errors.city && (
              <p className="mt-1.5 text-xs font-semibold text-rose-600">
                {errors.city}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="district" className="label">
              Quận/Huyện <span className="text-rose-500">*</span>
            </label>
            <input
              id="district"
              type="text"
              name="district"
              value={address.district}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Ví dụ: Quận 1"
              className={fieldClass(errors.district)}
              data-testid="district-input"
            />
            {errors.district && (
              <p className="mt-1.5 text-xs font-semibold text-rose-600">
                {errors.district}
              </p>
            )}
          </div>
        </div>

        <div className="grid grid-cols-1 gap-5 sm:grid-cols-3">
          <div>
            <label htmlFor="ward" className="label">
              Phường/Xã <span className="text-rose-500">*</span>
            </label>
            <input
              id="ward"
              type="text"
              name="ward"
              value={address.ward}
              onChange={handleChange}
              onBlur={handleBlur}
              placeholder="Ví dụ: Bến Nghé"
              className={fieldClass(errors.ward)}
              data-testid="ward-input"
            />
            {errors.ward && (
              <p className="mt-1.5 text-xs font-semibold text-rose-600">
                {errors.ward}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="postalCode" className="label">
              Mã Bưu điện
            </label>
            <input
              id="postalCode"
              type="text"
              name="postalCode"
              value={address.postalCode}
              onChange={handleChange}
              placeholder="70000"
              className="field"
              data-testid="postal-code-input"
            />
          </div>

          <div>
            <label htmlFor="phoneNumber" className="label">
              Số điện thoại <span className="text-rose-500">*</span>
            </label>
            <div className="relative">
              <FaPhoneAlt className="pointer-events-none absolute left-4 top-1/2 h-3 w-3 -translate-y-1/2 text-zinc-400" />
              <input
                id="phoneNumber"
                type="tel"
                name="phoneNumber"
                value={address.phoneNumber}
                onChange={handleChange}
                onBlur={handleBlur}
                placeholder="0901234567"
                maxLength={10}
                className={`${fieldClass(errors.phoneNumber)} pl-10`}
                data-testid="phone-number-input"
              />
            </div>
            {errors.phoneNumber && (
              <p className="mt-1.5 text-xs font-semibold text-rose-600">
                {errors.phoneNumber}
              </p>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}
