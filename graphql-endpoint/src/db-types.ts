/* eslint camelcase: ["error", {allow: ["new_balance", "changed_by", "from_to"]}] */
type Transaction = {
    id: number,
    iban: string,
    new_balance: string,
    changed_by: string,
    from_to: string,
    direction: string
    descr: string
}
