export class BusinessError extends Error {
  constructor(message, status = 409) {
    super(message);
    this.status = status;
  }
}

export function ensure(condition, message, status = 409) {
  if (!condition) throw new BusinessError(message, status);
}
