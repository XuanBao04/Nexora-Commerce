import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuthStore } from "@/store/useAuthStore";
import AdminLayout from "./AdminLayout";

const AdminDashboard = () => {
  const navigate = useNavigate();
  const { role } = useAuthStore();

  useEffect(() => {
    if (role !== "ROLE_ADMIN" && role !== "ADMIN") {
      navigate("/login", { replace: true });
      return;
    }
  }, [role, navigate]);

  return <AdminLayout />;
};

export default AdminDashboard;
