
def processFile(fileEntry,databag) {
    databag.log << "************\n"
    databag.log << " * FILE_INFO: ${fileEntry.fileEntryId} - ${fileEntry.title} \n"
}

def processFolder(folder, databag){
    databag.log << " PROCESSING Folder ******* ${folder}\n"

    // Get files and folders
    def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
    def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
    databag.log << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
    databag.log << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

    // Process each item
    files.each{ file ->
        processFile(file,databag)
    }

    folders.each { childFolder ->
        processFolder(childFolder,databag)
    }
    databag.log << " END PROCESSING Folder ******* ${folder}\n"
}

def searchFileEntry (folder, fileName, databag) {
    def file = null
    databag.log << " SEARCHING Folder ******* ${folder}\n"

    // Get files and folders
    def files = DLAppServiceUtil.getFileEntries(folder.groupId,folder.folderId)
    def folders = DLAppServiceUtil.getFolders(folder.groupId,folder.folderId)
    databag.log << " * FOLDER_INFO: ${folder.folderId} - ${folder.name}\n"
    databag.log << " * FOLDER_SIZE: Files: ${files.size()} / Folder: ${folders.size()}\n"

    // Process each item
    found = false
    for (int i=0; i < files.size(); i++) {
        file = files[i]
        databag.log << " comparison ${file.title} :: ${fileName} \n"
        if (file.title.equals(fileName)) {
            databag.log << "************\n"
            databag.log << " * FOUND : ${file.fileEntryId} - ${file.title} \n"
            found = true
            break;
        } else {
            file = null
        }
    }

    if (!found) {
        for (int k = 0; k < folders.size(); k++) {
            file = searchFileEntry(folders[k], fileName, databag)
            if (file != null)
                break;
        }
    }
    databag.log << " END PROCESSING Folder ******* ${folder}\n"
    return file
}
