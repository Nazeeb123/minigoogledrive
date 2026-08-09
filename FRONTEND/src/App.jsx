import { BrowserRouter, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Dashboard from "./pages/Dashboard";
import Folder from "./pages/Folder";
import Shared from "./pages/Shared";
import Starred from "./pages/Starred";
import Trash from "./pages/Trash";
import SearchPage from "./pages/SearchPage";
import AI from "./pages/AI";

function App() {


  return (
    <BrowserRouter>

      <Routes>

        <Route path="/login" element={<Login />} />

        <Route path="/register" element={<Register />} />

        <Route path="/dashboard" element={<Dashboard />} />

        <Route path="/folder/:id" element={<Folder />} />

        <Route path="/shared" element={<Shared />} />

        <Route path="/starred" element={<Starred />} />

        <Route path="/trash" element={<Trash />} />
        <Route
          path="/search"
          element={<SearchPage />}
        />
        <Route path="/ai" element={<AI />} />


      </Routes>

    </BrowserRouter>
  );
}

export default App;