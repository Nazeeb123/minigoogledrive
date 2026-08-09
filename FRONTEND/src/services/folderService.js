import axios from "axios";


const API = "http://localhost:8080/folders";

const token = () => localStorage.getItem("token");

export const getFolders = () => {
    return axios.get(`${API}/my`, {
        headers: {
            Authorization: `Bearer ${token()}`
        }
    });
};

export const createFolder = (folderName) => {
    return axios.post(
        `${API}/create?folderName=${folderName}`,
        {},
        {
            headers: {
                Authorization: `Bearer ${token()}`
            }
        }
    );
};

export const deleteFolder = (id) => {
    return axios.delete(`${API}/${id}`, {
        headers: {
            Authorization: `Bearer ${token()}`
        }
    });
};
