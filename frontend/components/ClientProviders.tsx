import { AuthProvider } from "@/hooks/useAuth";
import { ReactNode } from "react";
import { GoogleOAuthProvider } from "@react-oauth/google";

export default function ClientProviders({ children }: { children: ReactNode }) {
    const clientId = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || "MOCK_CLIENT_ID_FOR_DEV";

    return (
        <GoogleOAuthProvider clientId={clientId}>
            <AuthProvider>
                {children}
            </AuthProvider>
        </GoogleOAuthProvider>
    );
}
