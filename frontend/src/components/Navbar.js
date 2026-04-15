import { Link } from "react-router-dom";

function Navbar() {
  return (
    <div style={{ background: "#333", color: "white", padding: "10px" }}>
      <h2>DSM 🌐</h2>
      <Link to="/" style={{ color: "white", marginRight: "10px" }}>
        Home
      </Link>
      <Link to="/login" style={{ color: "white" }}>
        Login
      </Link>
    </div>
  );
}

export default Navbar;