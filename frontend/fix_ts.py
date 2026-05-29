import re
import os

files = [
    "/home/bao_phan/Projects/Nexora-commerce/frontend/src/features/admin/components/UserManagement.tsx",
    "/home/bao_phan/Projects/Nexora-commerce/frontend/src/features/products/components/ProductManagement.tsx",
    "/home/bao_phan/Projects/Nexora-commerce/frontend/src/features/products/components/CategoryManagement.tsx",
    "/home/bao_phan/Projects/Nexora-commerce/frontend/src/features/products/components/BrandManagement.tsx",
    "/home/bao_phan/Projects/Nexora-commerce/frontend/src/features/orders/components/OrderManagement.tsx"
]

for file in files:
    with open(file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. Fix unused imports: FaChevronLeft, FaChevronRight
    content = re.sub(r',\s*FaChevronLeft\s*', '', content)
    content = re.sub(r',\s*FaChevronRight\s*', '', content)

    # 2. Fix unused variables from useAdminPagination
    content = re.sub(r'nextPage\s*,\s*', '', content)
    content = re.sub(r'previousPage\s*,\s*', '', content)

    # 3. Ensure AdminPagination is imported
    if "import { AdminPagination }" not in content:
        # Find the useAdminPagination import and insert after it
        match = re.search(r'import \{ useAdminPagination \} .*?;', content)
        if match:
            content = content[:match.end()] + '\nimport { AdminPagination } from "@/features/admin/components/AdminPagination";' + content[match.end():]

    # 4. Fix updatePaginationData argument type issue
    content = re.sub(r'updatePaginationData\(([^.]+)\.pagination\)', r'updatePaginationData(\1.pagination as any)', content)

    with open(file, 'w', encoding='utf-8') as f:
        f.write(content)

print("Fixed TS errors in components.")
