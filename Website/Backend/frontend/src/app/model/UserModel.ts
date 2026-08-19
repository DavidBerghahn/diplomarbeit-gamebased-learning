export interface UserModel{
  id: number,
  username: string,
  favGame:Game,
  favConsole:Console
}

export interface Game{
  id:number,
  price:number,
  name:string
}

export interface Console{
  id:number,
  publicationDate:Date,
  companyName:string,
  modelName:string
}
