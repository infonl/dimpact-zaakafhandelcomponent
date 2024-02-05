import NodeFS from 'fs'

type Keys = 'caseNumber' | 'open-forms-testid'
export const testStorageFile = 'test-storage.json'
export class TestStorageService {
    constructor() {
        if (!NodeFS.existsSync(testStorageFile)) {
            NodeFS.writeFileSync(testStorageFile, '{}')
        }
    }

    public get(key: Keys) {
        const data = NodeFS.readFileSync(testStorageFile, 'utf8')
        const parsedData = JSON.parse(data)
        return parsedData[key]
    }

    public set(key: Keys, value: string) {
        const data = NodeFS.readFileSync(testStorageFile, 'utf8')
        const parsedData = JSON.parse(data)
        parsedData[key] = value
        NodeFS.writeFileSync(testStorageFile, JSON.stringify(parsedData))
    }
}