import { Link, useLocation } from "react-router-dom";

export const HeaderItem = ({ href, children }) => {
    const { pathname } = useLocation();

    console.log(pathname);

    return (
            <Link
                className={`${
                    pathname === href && "border-b-2 border-secondary font-bold"
                }`}
                to={href}
            >
                {children}
            </Link>
    );
};
