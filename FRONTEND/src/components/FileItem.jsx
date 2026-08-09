import { useState } from "react";
import { FaEllipsisV } from "react-icons/fa";
import "./FileItem.css";

function FileItem({ file, onRestore, onDeletePermanent }) {

  const [open, setOpen] = useState(false);

  return (
    <div className="file-item">

      <span>{file.fileName}</span>

      <FaEllipsisV
        className="menu-icon"
        onClick={() => setOpen(!open)}
      />

      {open && (
        <div className="dropdown-menu">
          <button onClick={() => onRestore(file.id)}>
            Restore
          </button>

          <button onClick={() => onDeletePermanent(file.id)}>
            Delete Permanently
          </button>
        </div>
      )}

    </div>
  );
}

export default FileItem;