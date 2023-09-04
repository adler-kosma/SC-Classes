SomeSuperclass {
    calculate { |in| ^in * 2 }
    value { |in| ^this.calculate(in) }
}

MyClass : SomeSuperclass {
    calculate { |in| ^in * 3 }
}