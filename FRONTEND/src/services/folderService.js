import API from "./api";

export const getFolders = () => {
    return API.get("/folders/my");
};

export const createFolder = (folderName) => {
    return API.post(
        `/folders/create?folderName=${encodeURIComponent(folderName)}`
    );
};

export const deleteFolder = (id) => {
    return API.delete(`/folders/${id}`);
};