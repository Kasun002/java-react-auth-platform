import { useNavigate } from "react-router";

const useGoBack = () => {
  const navigate = useNavigate();

  return () => {
      if (globalThis.history.state && globalThis.history.state.idx > 0) {
        navigate(-1); // Go back to the previous page
      } else {
        navigate("/"); // Redirect to home if no history exists
      }
    };
};

export default useGoBack;
