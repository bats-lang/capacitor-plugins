export interface HelloPlugin {
  /** Gives back the value it is given (Android only). */
  echo(options: { value: string }): Promise<{ value: string }>;
}
